@Test
fun `Given items, when requesting items with neither a limit nor a cursor, then every item must be retrieved`() {

}

@Test
fun `Given items, when requesting items with a limit and cursor, then the number of items specified by the limit must be returned from after the cursor`() {

}

@Test
fun `Given items, when requesting items with a limit but no cursor, then the number of items specified by the limit from the first item must be retrieved`() {

}

@Test
fun `Given items, when requesting items with a cursor but no limit, then every item after the cursor must be retrieved`() {

}

@Test
fun `Given items, when requesting items with the last item's cursor but no limit, then zero items must be retrieved, 'hasNextPage' must be 'false', and 'hasPreviousPage' must be 'true'`() {

}

@Test
fun `Given items 1-10 where item 4 has been deleted, when requesting the first three items after item 2, then items 3, 5, and 6 must be retrieved`() {

}

@Test
fun `Given items 1-10 where item 4 has been deleted, when requesting items using the deleted item's cursor, then items 5-10 must be retrieved`() {

}

@Test
fun `Retrieving the first of many items must cause 'hasNextPage' to be 'true', and 'hasPreviousPage' to be 'false'`() {

}

@Test
fun `Retrieving the last of many items must cause 'hasNextPage' to be 'false', and 'hasPreviousPage' to be 'true'`() {

}

@Test
fun `Given zero items, when requesting every item, then 'startCursor' must be 'null', 'endCursor' must be 'null', 'hasNextPage' must be 'false', and 'hasPreviousPage' must be 'false'`() {

}

@Test
fun `Given one item, when requesting every item, then 'startCursor' must point to the item, 'endCursor' must point to the item, 'hasNextPage' must be 'false', and 'hasPreviousPage' must be 'false'`() {

}

@Test
fun `Given items, when requesting zero items sans cursor, then 'hasNextPage' must be 'true', and 'hasPreviousPage' must be 'false'`() {

}

@Test
fun `Given items, when requesting zero items after the end cursor, then 'hasNextPage' must be 'false', and 'hasPreviousPage' must be 'true'`() {

}

@Test
fun `Given items, when requesting items after the start cursor, then 'hasNextPage' must be 'false', and 'hasPreviousPage' must be 'true'`() {

}

@Test
fun `Given items 1-10, when requesting zero items after item 5, then 'hasNextPage' must be 'true', and 'hasPreviousPage' must be 'true'`() {

}

@Test
fun `Given items, when requesting the start and end cursors, then they must point to the first and last items respectively`() {

}

@Test
fun `Given items 1-10 where items 1-5 have been deleted, when requesting zero items after the deleted item 3, then 'hasNextPage' must be 'true', and 'hasPreviousPage' must be 'false'`() {

}

@Test
fun `Given items 1-10 where items 6-10 have been deleted, when requesting items after the deleted item 7, then 'hasNextPage' must be 'false', and 'hasPreviousPage' must be 'true'`() {

}
