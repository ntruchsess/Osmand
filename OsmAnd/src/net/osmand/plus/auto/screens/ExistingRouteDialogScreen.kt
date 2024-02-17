package net.osmand.plus.auto.screens

import androidx.car.app.CarContext
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.Action
import androidx.car.app.model.Header
import androidx.car.app.model.Pane
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.MapTemplate
import net.osmand.plus.search.listitems.QuickSearchListItem
import net.osmand.search.core.SearchResult

/**
 * Screen for showing a list of favorite places.
 */
class ExistingRouteDialogScreen(
    carContext: CarContext,
    private val settingsAction: Action,
    private val searchResult: SearchResult
) : BaseAndroidAutoScreen(carContext) {

    override fun onGetTemplate(): Template {
        return MapTemplate.Builder()
            .setHeader(
                Header.Builder()
                    .setTitle("New destination")
                    .setStartHeaderAction(Action.BACK)
                    .build()
            )
            .setPane(Pane.Builder()
                .addRow(Row.Builder()
                    .setTitle(searchResult.localeName)
                    .addText("Add to existing route or replace?")
                    .build()
                )
                .addAction(Action.Builder()
                    .setTitle("Add")
                    .setOnClickListener {
                        setResult(Result.ADD_TO_ROUTE)
                        finish()
                    }
                    .build())
                .addAction(Action.Builder()
                    .setTitle("Replace")
                    .setOnClickListener {
                        setResult(Result.REPLACE_ROUTE)
                        finish()
                    }
                    .build())
                .build())
            .build()
    }

    override fun getConstraintLimitType(): Int {
        return ConstraintManager.CONTENT_LIMIT_TYPE_PANE
    }

    enum class Result {
        REPLACE_ROUTE,
        ADD_TO_ROUTE
    }
}
