package io.thernal.pagingkit.sample.shared.fake

/** What a failed request throws; a real loader would let its transport exception through. */
class FakeNetworkException(message: String) : Exception(message)
