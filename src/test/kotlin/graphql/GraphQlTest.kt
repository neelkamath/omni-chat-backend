package com.neelkamath.omniChat.test.graphql

import com.neelkamath.omniChat.graphql.buildDateTimeScalar
import graphql.language.StringValue
import graphql.schema.Coercing
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.*

class GraphQlTest : StringSpec({
    val coercing: Coercing<Any?, Any?> = buildDateTimeScalar().coercing
    val iso8601DateTime = "2020-04-24T02:18:51.791Z"
    val date: Date = Calendar.getInstance().apply {
        set(Calendar.YEAR, 2020)
        set(Calendar.MONTH, Calendar.APRIL)
        set(Calendar.DAY_OF_MONTH, 24)
        set(Calendar.HOUR, 2)
        set(Calendar.MINUTE, 18)
        set(Calendar.SECOND, 51)
        set(Calendar.MILLISECOND, 791)
        set(Calendar.AM_PM, Calendar.AM)
    }.time

    "A value should be parsed" { coercing.parseValue(iso8601DateTime) shouldBe date }

    "A literal should be parsed" { coercing.parseLiteral(StringValue(iso8601DateTime)) shouldBe date }

    "A Date should be serialized" { coercing.serialize(date) shouldBe iso8601DateTime }
})