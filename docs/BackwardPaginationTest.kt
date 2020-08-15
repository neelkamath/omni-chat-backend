class BackwardPaginationTest {
    @Test
    fun `Every item should be retrieved if neither cursor nor limit get supplied`() {

    }

    @Test
    fun `Using a deleted item's cursor should cause pagination to work as if the item still exists`() {

    }

    @Test
    fun `Retrieving the first of many items should cause the page info to state there are only items after it`() {

    }

    @Test
    fun `Retrieving the last of many items should cause the page info to state there are only items before it`() {

    }

    @Test
    fun `The start and end cursors should be null if there are no items`() {

    }

    @Test
    fun `The start and end cursors should be the same if there's only one item`() {

    }

    @Test
    fun `The first and last cursors should be the first and last items respectively`() {

    }

    @Test
    fun `The number of items specified by the limit should be returned from before the cursor`() {

    }

    @Test
    fun `The number of items specified by the limit from the last item should be retrieved when there's no cursor`() {

    }

    @Test
    fun `Every item before the cursor should be retrieved when there's no limit`() {

    }
}