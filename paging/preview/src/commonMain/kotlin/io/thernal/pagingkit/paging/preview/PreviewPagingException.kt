package io.thernal.pagingkit.paging.preview

/** The throwable preview states carry; its message is what an error slot showing it will print. */
class PreviewPagingException(
    message: String = "Preview error",
) : Exception(message)
