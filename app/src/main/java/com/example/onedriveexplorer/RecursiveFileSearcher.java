package com.example.onedriveexplorer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class RecursiveFileSearcher {

    private final MutableLiveData<List<File>> searchResults = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isSearchingLiveData = new MutableLiveData<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private AtomicBoolean isSearching = new AtomicBoolean(false);

    public LiveData<List<File>> getSearchResults() {
        return searchResults;
    }

    public LiveData<Boolean> getIsSearching() {
        return isSearchingLiveData;
    }

    public void search(File rootDir, String query) {
        // Cancel previous search
        isSearching.set(false);
        
        if (query == null || query.isEmpty()) {
            searchResults.postValue(new ArrayList<>());
            isSearchingLiveData.postValue(false);
            return;
        }

        isSearching.set(true);
        isSearchingLiveData.postValue(true);
        searchResults.postValue(new ArrayList<>()); // Clear previous results

        executor.submit(() -> {
            List<File> results = new ArrayList<>();
            recursiveSearch(rootDir, query.toLowerCase(), results);
            isSearching.set(false);
            isSearchingLiveData.postValue(false);
        });
    }

    private void recursiveSearch(File dir, String query, List<File> currentResults) {
        if (!isSearching.get()) return;

        File[] files = dir.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (!isSearching.get()) return;

            if (f.getName().toLowerCase().contains(query)) {
                currentResults.add(f);
                // Post intermediate results
                // To avoid overwhelming the UI, maybe post copy?
                // LiveData postValue is safe for background threads but drops events if called too fast.
                // For a smooth "live" list, this is usually acceptable, though we might miss some intermediate "frames" of the list growing.
                // Ideally, debouncing or batching updates is better, but this simple approach works for many cases.
                // We must create a NEW list instance because if we modify the old one, DiffUtil (if used) or Observers might see same object.
                searchResults.postValue(new ArrayList<>(currentResults));
            }

            if (f.isDirectory()) {
                recursiveSearch(f, query, currentResults);
            }
        }
    }

    public void stopSearch() {
        isSearching.set(false);
    }
}
