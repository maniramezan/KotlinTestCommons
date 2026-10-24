package io.github.maniramezan.testcommons.samples.android

internal data class CatalogState(
    val isLoading: Boolean = true,
    val lessons: List<String> = emptyList(),
    val error: String? = null,
)
