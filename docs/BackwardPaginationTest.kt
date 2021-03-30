class BackwardPaginationTest {
    @Test
    fun `Every item must be retrieved if neither cursor nor limit get supplied`() {

    }

    @Test
    fun `Using a deleted item's cursor must cause pagination to work as if the item still exists`() {

    }

    @Test
    fun `Retrieving the first of many items must cause the page info to state there are only items after it`() {

    }

    @Test
    fun `Retrieving the last of many items must cause the page info to state there are only items before it`() {

    }

    @Test
    fun `If there are no items, the page info must indicate such`() {

    }

    @Test
    fun `If there's one item, the page info must indicate such`() {

    }

    @Test
    fun `The first and last cursors must be the first and last items respectively`() {

    }

    @Test
    fun `The number of items specified by the limit must be returned from before the cursor`() {

    }

    @Test
    fun `The number of items specified by the limit from the last item must be retrieved when there's no cursor`() {

    }

    @Test
    fun `Every item before the cursor must be retrieved when there's no limit`() {

    }
}
