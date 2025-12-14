// --- Imports ---
import http from 'http';
import { URL, URLSearchParams } from 'url';
import crypto from 'crypto';
import fetch from 'node-fetch'; 
import os from 'os'; // New import to find the host IP for logging

// --- Configuration ---
const CONFIG = {
    CLIENT_ID: 'CLIENT_ID', // <-- REPLACE THIS WITH YOUR ACTUAL CLIENT ID
    REDIRECT_URI: 'http://localhost:8010/auth/callback',
    SCOPES: 'Files.ReadWrite.All offline_access User.Read',
    AUTHORITY: 'https://login.microsoftonline.com/common/oauth2/v2.0',
    PORT: 8010,
    HOST: '0.0.0.0', // Listen on all interfaces
    TOKEN_ENDPOINT: '/token', 
    HOSTED_TOKEN: null, 
    // New flag to ensure token exchange only happens once
    TOKEN_EXCHANGED: false 
};

// --- PKCE & State Generation ---
function generateRandomString(length) {
    return crypto.randomBytes(Math.ceil(length / 2))
        .toString('hex') 
        .slice(0, length);
}
function generateCodeChallenge(code_verifier) {
    const hash = crypto.createHash('sha256').update(code_verifier).digest('base64');
    return hash.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

function getHostIP() {
    const interfaces = os.networkInterfaces();
    for (const name in interfaces) {
        for (const iface of interfaces[name]) {
            if (iface.family === 'IPv4' && !iface.internal) {
                return iface.address;
            }
        }
    }
    return '127.0.0.1';
}

// --- OAuth Flow Functions ---

function initiateAuth(code_verifier, code_challenge, state) {
    const params = new URLSearchParams({
        client_id: CONFIG.CLIENT_ID,
        response_type: 'code',
        redirect_uri: CONFIG.REDIRECT_URI,
        scope: CONFIG.SCOPES,
        state: state,
        code_challenge: code_challenge,
        code_challenge_method: 'S256',
    });

    const authUrl = `${CONFIG.AUTHORITY}/authorize?${params.toString()}`;
    
    console.log('--- Step 1: User Authorization ---');
    console.log(`Please open this URL in your web browser:`);
    console.log(`\n${authUrl}\n`);
    console.log(`The browser will redirect back to http://localhost:${CONFIG.PORT}/auth/callback after sign-in.`);
    return authUrl;
}

async function exchangeCodeForToken(authorization_code, code_verifier) {
    // (Token exchange logic remains the same)
    const tokenUrl = `${CONFIG.AUTHORITY}/token`;
    const body = new URLSearchParams({
        grant_type: 'authorization_code',
        client_id: CONFIG.CLIENT_ID,
        scope: CONFIG.SCOPES,
        code: authorization_code,
        redirect_uri: CONFIG.REDIRECT_URI,
        code_verifier: code_verifier,
    });

    const response = await fetch(tokenUrl, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: body,
    });

    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Token exchange failed: ${response.status} - ${errorText}`);
    }
    return response.json();
}

function startServer(code_verifier, expected_state) {
    const hostIP = getHostIP(); 

    return new Promise((resolve, reject) => {
        const server = http.createServer(async (req, res) => {
            const requestUrl = new URL(req.url, `http://${req.headers.host}`);
            
            // --- A. Handle the OAuth Callback Redirect ---
            if (requestUrl.pathname === '/auth/callback' && req.method === 'GET' && !CONFIG.TOKEN_EXCHANGED) {
                const code = requestUrl.searchParams.get('code');
                const state = requestUrl.searchParams.get('state');
                
                // ... (Error handling) ...
                if (state !== expected_state || requestUrl.searchParams.has('error')) {
                    // Simple error response, server stays running
                    res.writeHead(400, { 'Content-Type': 'text/plain' });
                    res.end('Authentication failed or state mismatch.');
                    return;
                }
                
                // Send success message to the browser
                res.writeHead(200, { 'Content-Type': 'text/html' });
                res.end(`<h1>Authentication Successful!</h1><p>Token exchange initiated. Token is available for your app at <strong>http://${hostIP}:${CONFIG.PORT}${CONFIG.TOKEN_ENDPOINT}</strong></p>`);

                try {
                    console.log('\n--- Step 3: Exchanging Code for Token ---');
                    const tokenData = await exchangeCodeForToken(code, code_verifier);
                    CONFIG.HOSTED_TOKEN = tokenData; 
                    CONFIG.TOKEN_EXCHANGED = true; // Set flag to prevent re-exchange

                    console.log(`\n✅ Token exchange complete. Server remains running.`);
                    console.log(`   Access the token at: http://${hostIP}:${CONFIG.PORT}${CONFIG.TOKEN_ENDPOINT}`);

                } catch (e) {
                    // On exchange failure, the server remains running, but HOSTED_TOKEN is null
                    console.error('\n--- TOKEN EXCHANGE FAILED ---');
                    console.error(e.message);
                }

            // --- B. Handle the /token request from the Android app ---
            } else if (requestUrl.pathname === CONFIG.TOKEN_ENDPOINT && req.method === 'GET') {
                if (CONFIG.HOSTED_TOKEN) {
                    console.log(`\n---> Token requested and served at ${new Date().toLocaleTimeString()}`);
                    res.writeHead(200, { 
                        'Content-Type': 'application/json',
                        'Access-Control-Allow-Origin': '*', 
                    });
                    res.end(JSON.stringify(CONFIG.HOSTED_TOKEN));
                } else {
                    res.writeHead(404, { 'Content-Type': 'text/plain' });
                    res.end('Token not yet available. Please complete browser authentication first.');
                }

            } else {
                 res.writeHead(404, { 'Content-Type': 'text/plain' });
                 res.end('Not Found');
            }
        });

        server.listen(CONFIG.PORT, CONFIG.HOST, () => {
            console.log(`\n--- Server running on ${CONFIG.HOST}:${CONFIG.PORT} ---`);
            console.log(`   Local network access IP: http://${hostIP}:${CONFIG.PORT}`);
        }).on('error', (err) => {
            reject(new Error(`Server failed to start on port ${CONFIG.PORT}: ${err.message}`));
        });
    });
}

// --- Main Execution ---
async function main() {
    try {
        if (CONFIG.CLIENT_ID === 'YOUR_CLIENT_ID_HERE') {
            throw new Error("Configuration Error: Please update CONFIG.CLIENT_ID with your actual Azure Application ID.");
        }
        
        const code_verifier = generateRandomString(128); 
        const code_challenge = generateCodeChallenge(code_verifier);
        const state = generateRandomString(32); 
        
        initiateAuth(code_verifier, code_challenge, state);

        // Await the promise but don't force termination on resolve
        await startServer(code_verifier, state);
        
    } catch (error) {
        console.error('\n--- FATAL ERROR IN OAUTH FLOW ---');
        console.error(error.message);
    }
}

main();