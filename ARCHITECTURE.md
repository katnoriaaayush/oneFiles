# Project Architecture: OneDrive File Explorer

## Overview
This project is an Android application acting as a file explorer for OneDrive. It allows users to browse their OneDrive files, navigate through folders, view files in list or grid modes, and perform basic file operations (Download, Rename, Delete, Create Folder).

The application is built using **Java** and follows a **Model-View-Controller (MVC)** pattern, with the `MainActivity` acting as the controller and a specialized `NavigationManager` handling the directory traversal logic.

## Architecture Patterns

### 1. Navigation & State Management
The core of the application's unique architecture is the **Tree-Based Navigation System**.
Instead of pushing new Fragments/Activities for every folder, the app stays in `MainActivity` and updates a single `RecyclerView`.

*   **`NavigationManager`**: This class manages the state of the current directory.
    *   **`FileNode`**: Represents a directory in the tree. It holds:
        *   The `DriveItem` (metadata).
        *   A reference to its `parentNode` (for back navigation).
        *   A cached list of `children` (items inside the folder).
    *   **Caching Strategy**: When a user visits a folder, the results are cached in the `FileNode`. Searching the same folder again typically loads from memory instantly. `refresh()` clears this cache to force a network reload.

### 2. Network Layer
The app uses **Retrofit** for network requests.
*   **`OneDriveService`**: Defines the API endpoints (GET children, POST folder, DELETE item, etc.).
*   **`TokenService`**: Handles fetching the OAuth access token from a local/specified server.
*   **`OneDriveClient`**: A singleton/static helper to manage the `Retrofit` instance and inject the Access Token into requests.

### 3. UI Layer
*   **`MainActivity`**: The central hub. It initializes the `NavigationManager`, observes its callbacks (`onSuccess`, `onError`, `onLoading`), and updates the UI accordingly.
*   **`FileAdapter`**: A generic `RecyclerView` adapter that supports toggling between **List** and **Grid** view types dynamically.

## Key Components

### `app/src/main/java/com/example/onedriveexplorer`

| Component | Responsibility |
| :--- | :--- |
| **`MainActivity`** | Handles UI setup, event listeners (clicks, dialogs), and connects `NavigationManager` to the `RecyclerView`. |
| **`FileAdapter`** | Binds `DriveItem` data to the views. Handles toggling between `item_file_list.xml` and `item_file_grid.xml`. |

### `.../navigation`

| Component | Responsibility |
| :--- | :--- |
| **`NavigationManager`** | The "Brain" of the explorer. Handles `navigateTo(item)` and `goBack()`. Manages the cache via `FileNode`. |
| **`FileNode`** | Data structure representing a node in the navigation tree. Holds the cache of its children. |

### `.../models`

| Component | Responsibility |
| :--- | :--- |
| **`DriveItem`** | POJO representing a file or folder from the Microsoft Graph API. |
| **`DriveItemResponse`** | Wrapper for the standard Graph API response format (contains a list of `DriveItem`). |

### `.../network`

| Component | Responsibility |
| :--- | :--- |
| **`OneDriveService`** | Retrofit interface for Microsoft Graph API calls. |
| **`TokenService`** | Interface for fetching the initial authentication token. |

## Data Flow

1.  **Initialization**:
    *   `MainActivity` calls `fetchToken()` to get an Access Token.
    *   On success, `OneDriveClient.setAccessToken(token)` is called.
    *   `initNavigation()` creates a "Root" `FileNode` and starts the `NavigationManager`.

2.  **Navigation (`NavigationManager.navigateTo`)**:
    *   **Input**: User clicks a folder.
    *   **Check**: Does a `FileNode` for this folder exist in memory?
    *   **Cache Hit**: If yes and data is cached -> Return cached data immediately (`onSuccess`).
    *   **Cache Miss**: If no -> Call `OneDriveService.listChildren()`.
        *   On API Success -> Cache data in the `FileNode` -> Return data (`onSuccess`).

3.  **Back Navigation**:
    *   User presses Back functionality.
    *   `NavigationManager` moves `currentNode` pointer to `parentNode`.
    *   Since parent was visited, data is likely cached and displays instantly.

4.  **File Operations**:
    *   User triggers action (e.g., "Create Folder").
    *   `MainActivity` shows a Dialog for input.
    *   `OneDriveService` API called.
    *   On Success -> `nav.refresh()` is called to reload the current folder data from server.

## Dependencies
*   **Retrofit 2**: For REST API calls.
*   **Gson**: For JSON parsing.
*   **SwipeRefreshLayout**: For pull-to-refresh functionality.
*   **Material Design**: For FAB, Dialogs, and UI components.
