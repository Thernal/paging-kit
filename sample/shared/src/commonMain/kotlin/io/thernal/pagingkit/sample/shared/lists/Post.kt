package io.thernal.pagingkit.sample.lists

data class Post(
    val id: Int,
    val author: String,
    val text: String,
)

private val authors = listOf("Aysel", "Kamran", "Leyla", "Murad", "Nigar", "Orxan")

internal fun seedPosts(count: Int): List<Post> {
    return (1..count).map { id ->
        Post(
            id = id,
            author = authors.getOrElse(index = id % authors.size) { "Someone" },
            text = "Post #$id — something worth scrolling past.",
        )
    }
}
