package io.github.maniramezan.testcommons.samples.android

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class CatalogModel(private val fetch: suspend () -> List<String>) {
    private val current = MutableStateFlow(CatalogState())
    val state: StateFlow<CatalogState> = current.asStateFlow()

    suspend fun load() {
        current.value = CatalogState()
        try {
            current.value = CatalogState(isLoading = false, lessons = fetch())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            current.value = CatalogState(isLoading = false, error = "Could not load lessons. Try again.")
        }
    }
}
