package net.osmand.plus.auto.screens

import android.text.SpannableString
import androidx.car.app.CarContext
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.Action
import androidx.car.app.model.Distance
import androidx.car.app.model.DistanceSpan
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.Row
import androidx.car.app.navigation.model.PlaceListNavigationTemplate
import net.osmand.search.core.SearchResult

/**
 * Screen for showing a list of favorite places.
 */
class AddToRouteScreen(
    carContext: CarContext,
    private val result: SearchResult
) : BaseAndroidAutoScreen(carContext) {

    override fun onGetTemplate() =
        PlaceListNavigationTemplate.Builder()
            .setHeader(
                Header.Builder()
                    .setTitle("Insert Foo after")
                    .setStartHeaderAction(Action.BACK)
                    .build()
            )
            .setItemList(
                app.targetPointsHelper.allPoints.foldIndexed(
                    ItemList.Builder().addItem(createRow("Current location", 0.0, 0))) {
                        index, builder, point -> builder.addItem(createRow(point.onlyName, 1.0, index + 1))
                }.build()
            )
            .build();

    private fun createRow(title: String, distance: Double, index: Int) : Row = Row.Builder()
        .setTitle(title)
        .addText(SpannableString(" ").also {
            it.setSpan(
                DistanceSpan.create(
                    Distance.create(
                        distance,
                        Distance.UNIT_KILOMETERS
                    )
                ),
                0,
                1,
                SpannableString.SPAN_INCLUSIVE_INCLUSIVE
            )
        })
        .setOnClickListener {
            setResult(index)
            finish()
        }
        .build()

    override fun getConstraintLimitType(): Int {
        return ConstraintManager.CONTENT_LIMIT_TYPE_PANE
    }
}
