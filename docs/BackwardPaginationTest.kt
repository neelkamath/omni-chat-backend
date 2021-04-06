@Test
fun `Every item must be retrieved if neither cursor nor limit get supplied`() {

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

@Test
fun `Zero items must be retrieved along with the correct 'hasNextPage' and 'hasPreviousPage' when using the first item's cursor`() {

}

@Test
fun `Given items 1-10 where item 4 has been deleted, when requesting the last three items before item 6, then items 2, 3, and 5 must be retrieved`() {

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
fun `If there are zero items, the page info must indicate such`() {

}

@Test
fun `If there's one item, the page info must indicate such`() {

}

@Test
fun `When requesting zero items sans cursor, the 'hasNextPage' and 'hasPreviousPage' must indicate such`() {

}

@Test
fun `When requesting zero items before the start cursor, the 'hasNextPage' and 'hasPreviousPage' must indicate such`() {

}

@Test
fun `When requesting items before the end cursor, 'hasNextPage' must be 'true', and 'hasPreviousPage' must be 'false'`() {

}

@Test
fun `Given items 1-10, when requesting zero items before item 5, the 'hasNextPage' and 'hasPreviousPage' must indicate such`() {

}

@Test
fun `The first and last cursors must be the first and last items respectively`() {

}
