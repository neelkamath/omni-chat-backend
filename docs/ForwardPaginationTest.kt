@Test
fun `Every item must be retrieved if neither cursor nor limit get supplied`() {

}

@Test
fun `The number of items specified by the limit must be returned from after the cursor`() {

}

@Test
fun `The number of items specified by the limit from the first item must be retrieved when there's no cursor`() {

}

@Test
fun `Every item after the cursor must be retrieved when there's no limit`() {

}

@Test
fun `Zero items must be retrieved along with the correct 'hasNextPage' and 'hasPreviousPage' when using the last item's cursor`() {

}

@Test
fun `Given items 1-10 where item 4 has been deleted, when requesting the first three items after item 2, then items 3, 5, and 6 must be retrieved`() {

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
fun `When requesting zero items after the end cursor, the 'hasNextPage' and 'hasPreviousPage' must indicate such`() {

}

@Test
fun `When requesting items after the start cursor, 'hasNextPage' must be 'false', and 'hasPreviousPage' must be 'true'`() {

}

@Test
fun `Given items 1-10, when requesting zero items after item 5, the 'hasNextPage' and 'hasPreviousPage' must indicate such`() {

}

@Test
fun `The first and last cursors must be the first and last items respectively`() {

}
