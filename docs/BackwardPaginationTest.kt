class BackwardPaginationTest : FunSpec({
    test("Every item should be retrieved if neither cursor nor limit get supplied") {

    }

    test("Using a deleted item's cursor should cause pagination to work as if the item still exists") {

    }

    test(
        """
        Given multiple items,
        when retrieving only the first item,
        then the page's info should state that there are items after, but none before, the first item
        """
    ) {

    }

    test(
        """
        Given multiple items,
        when retrieving only the last item,
        then the page's info should state that there are items before, but none after, the last item
        """
    ) {

    }

    test("The start and end cursors should be null if there are no items") {

    }

    test("The start and end cursors should be the same if there's only one item") {

    }

    test("The first and last cursors should be the first and last items respectively") {

    }

    test(
        """
        Given both a limit and cursor, 
        when retrieving items, 
        then the number of items specified by the limit should be returned from before the cursor
        """
    ) {

    }

    test(
        """
        Given a limit without a cursor, 
        when retrieving items, 
        then the number of items specified by the limit from the last item should be retrieved
        """
    ) {

    }

    test(
        """
        Given a cursor without a limit, 
        when retrieving items, 
        then every item before the cursor should be retrieved
        """
    ) {

    }
})