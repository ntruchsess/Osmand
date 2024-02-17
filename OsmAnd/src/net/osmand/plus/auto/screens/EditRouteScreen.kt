package net.osmand.plus.auto.screens

import androidx.car.app.CarContext
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.PlaceListNavigationTemplate
import androidx.core.graphics.drawable.IconCompat
import net.osmand.plus.R

/**
 * Screen for showing a list of favorite places.
 */
class EditRouteScreen(
    carContext: CarContext,
) : BaseAndroidAutoScreen(carContext) {

    override fun onGetTemplate(): Template {
        return PlaceListNavigationTemplate.Builder()
            .setHeader(
                Header.Builder()
                    .setTitle("Select Location to move")
                    .setStartHeaderAction(Action.BACK)
                    .build()
            )
            .setItemList(ItemList.Builder()
                .addItem(Row.Builder()
                    .addText("fist Destination")
                    .addAction(Action.Builder()
                        .setIcon(
                            CarIcon.Builder(
                                IconCompat.createWithResource(
                                    carContext, R.drawable.ic_action_delete_item
                                )
                            ).build())
                        .build())
                    .build())
                .addItem(Row.Builder()
                    .addText("final Destination")
                    .addAction(Action.Builder()
                        .setIcon(
                            CarIcon.Builder(
                                IconCompat.createWithResource(
                                    carContext, R.drawable.ic_action_delete_item
                                )
                            ).build())
                        .build())
                    .build())
                .build())
            .build();
    }

    override fun getConstraintLimitType(): Int {
        return ConstraintManager.CONTENT_LIMIT_TYPE_PANE
    }
}
