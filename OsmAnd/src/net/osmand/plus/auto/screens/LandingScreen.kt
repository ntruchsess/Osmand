package net.osmand.plus.auto.screens

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Distance
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.Destination
import androidx.car.app.navigation.model.MessageInfo
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.navigation.model.PlaceListNavigationTemplate
import androidx.car.app.navigation.model.RoutingInfo
import androidx.car.app.navigation.model.Step
import androidx.car.app.navigation.model.TravelEstimate
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import net.osmand.data.ValueHolder
import net.osmand.plus.R
import net.osmand.plus.auto.NavigationListener
import net.osmand.plus.auto.NavigationSession
import net.osmand.plus.auto.SurfaceRenderer
import net.osmand.plus.routing.IRouteInformationListener
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings
import net.osmand.plus.views.mapwidgets.widgets.AlarmWidget
import net.osmand.plus.views.mapwidgets.widgets.SpeedometerWidget
import net.osmand.util.Algorithms

class LandingScreen(
    carContext: CarContext,
    private val settingsAction: Action,
    private val listener: NavigationListener) :
    BaseAndroidAutoScreen(carContext),
    SurfaceRenderer.SurfaceRendererCallback,
    IRouteInformationListener,
    DefaultLifecycleObserver {

    var isNavigationTemplate = false

    private var navigating = false
    private var rerouting = false
    private var arrived = false
    private var destinations: List<Destination>? = null
    private var steps: List<Step>? = null
    private var stepRemainingDistance: Distance? = null
    private var destinationTravelEstimate: TravelEstimate? = null
    private var shouldShowNextStep = false
    private var shouldShowLanes = false
    private var junctionImage: CarIcon? = null

    private val alarmWidget = AlarmWidget(app, null)
    private val speedometerWidget = SpeedometerWidget(app, null)

    @DrawableRes
    private var compassResId = R.drawable.ic_compass_niu

    init {
        lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        app.routingHelper.addListener(this)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        adjustMapPosition(false)
        app.routingHelper.removeListener(this)
        lifecycle.removeObserver(this)
    }

    override fun onFrameRendered(canvas: Canvas, visibleArea: Rect, stableArea: Rect) {
        surfaceRenderer?.also { renderer ->
            val drawSettings = DrawSettings(carContext.isDarkMode, false, renderer.density)
            alarmWidget.updateInfo(drawSettings, true)
            speedometerWidget.updateInfo(drawSettings, true, drawSettings.isNightMode)
            val alarmBitmap = alarmWidget.widgetBitmap
            val speedometerBitmap = speedometerWidget.widgetBitmap
            speedometerBitmap?.also { bitmap ->
                canvas.drawBitmap(
                    bitmap,
                    (visibleArea.right - bitmap.width - 10).toFloat(),
                    (visibleArea.top + 10).toFloat(),
                    Paint()
                )
            }
            alarmBitmap?.also { bitmap ->
                val offset = speedometerBitmap?.width ?: 0
                canvas.drawBitmap(
                    bitmap,
                    (visibleArea.right - bitmap.width - 10 - offset).toFloat(),
                    (visibleArea.top + 10).toFloat(),
                    Paint()
                )
            }
        }
    }

    override fun newRouteIsCalculated(newRoute: Boolean, showToast: ValueHolder<Boolean?>?) {
        val app = app
        val map = app.osmandMap
        val rh = app.routingHelper
        if (rh.isRoutePlanningMode) {
            adjustMapPosition(true)
        }
        map.refreshMap()
        if (newRoute && rh.isRoutePlanningMode && map.mapView.isCarView) {
            app.runInUIThread({ app.osmandMap.fitCurrentRouteToMap(false, 0) }, 300)
        }
    }

    override fun routeWasCancelled() {}

    override fun routeWasFinished() {}

    override fun onGetTemplate(): Template {
        return if (isNavigationTemplate) onGetNavigationScreenTemplate() else onGetLandingScreenTemplate()
    }

    private fun onGetLandingScreenTemplate(): Template {

        updateCompass()

        val header = Header.Builder()
            .setTitle(app.getString(R.string.app_name))
            .setStartHeaderAction(Action.APP_ICON)
            .build()

        val app = app
        val itemList = PlaceCategory.values().asSequence().fold(ItemList.Builder()) { builder, category ->
            val title = app.getString(category.titleId)
            val icon = CarIcon.Builder(IconCompat.createWithResource(app, category.iconId)).build()
            builder.addItem(
                Row.Builder()
                    .setTitle(title)
                    .setImage(icon)
                    .setBrowsable(true)
                    .setOnClickListener { onCategoryClick(category) }
                    .build())
        }
            .build()

        val actionStrip = ActionStrip.Builder()
            .addAction(settingsAction)
            .addAction(createSearchAction())
            .build()

        val mapActionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder(Action.PAN).build()
            )
            .addAction(
                Action.Builder()
                    .setIcon(
                        CarIcon.Builder(
                            IconCompat.createWithResource(
                                carContext,
                                R.drawable.ic_my_location
                            ))
                            .build()
                    )
                    .setOnClickListener {
                        session?.navigationCarSurface?.handleRecenter()
                    }
                    .build())
            .addAction(
                Action.Builder()
                    .setIcon(
                        CarIcon.Builder(
                            IconCompat.createWithResource(
                                carContext,
                                R.drawable.ic_zoom_in
                            ))
                            .build()
                    )
                    .setOnClickListener {
                        app.carNavigationSession?.navigationCarSurface?.handleScale(
                            NavigationSession.INVALID_FOCAL_POINT_VAL,
                            NavigationSession.INVALID_FOCAL_POINT_VAL,
                            NavigationSession.ZOOM_IN_BUTTON_SCALE_FACTOR
                        )
                    }
                    .build())
            .addAction(
                Action.Builder()
                    .setIcon(
                        CarIcon.Builder(
                            IconCompat.createWithResource(
                                carContext,
                                R.drawable.ic_zoom_out
                            ))
                            .build()
                    )
                    .setOnClickListener {
                        app.carNavigationSession?.navigationCarSurface?.handleScale(
                            NavigationSession.INVALID_FOCAL_POINT_VAL,
                            NavigationSession.INVALID_FOCAL_POINT_VAL,
                            NavigationSession.ZOOM_OUT_BUTTON_SCALE_FACTOR
                        )
                    }
                    .build())
            .build()

        return PlaceListNavigationTemplate.Builder()
            .setHeader(header)
            .setItemList(itemList)
            .setMapActionStrip(mapActionStrip)
            .setActionStrip(actionStrip)
            .build()
    }

    private fun onCategoryClick(category: PlaceCategory) {
        when (category) {
            PlaceCategory.FREE_MODE -> {
                isNavigationTemplate = true
                invalidate()
            }

            PlaceCategory.FAVORITES -> screenManager.push(
                FavoriteGroupsScreen(
                    carContext,
                    settingsAction
                )
            )

            PlaceCategory.HISTORY -> screenManager.push(
                HistoryScreen(
                    carContext,
                    settingsAction)
            )

            PlaceCategory.MAP_MARKERS -> screenManager.push(
                MapMarkersScreen(
                    carContext,
                    settingsAction)
            )

            PlaceCategory.POI -> screenManager.push(
                POICategoriesScreen(
                    carContext,
                    settingsAction)
            )

            PlaceCategory.TRACKS -> screenManager.push(
                TracksFoldersScreen(
                    carContext,
                    settingsAction)
            )
        }
    }

    internal enum class PlaceCategory(
        @DrawableRes val iconId: Int,
        @StringRes val titleId: Int) {
        FREE_MODE(R.drawable.ic_action_start_navigation, R.string.free_ride),
        HISTORY(R.drawable.ic_action_history, R.string.shared_string_history),
        POI(R.drawable.ic_action_info_dark, R.string.poi_categories),
        FAVORITES(R.drawable.ic_action_favorite, R.string.shared_string_favorites),
        MAP_MARKERS(R.drawable.ic_action_flag_stroke, R.string.map_markers_item),
        TRACKS(R.drawable.ic_action_polygom_dark, R.string.shared_string_gpx_tracks);
    }

    private fun onGetNavigationScreenTemplate(): Template {

        updateCompass()

        val navigationBuilder = NavigationTemplate.Builder()
        navigationBuilder.setBackgroundColor(CarColor.SECONDARY)

        // Set the action strip.
        navigationBuilder.setActionStrip(
            ActionStrip.Builder().let { actionStripBuilder ->
                if (!navigating) {
                    actionStripBuilder.addAction(
                        Action.Builder()
                            .setIcon(
                                CarIcon.Builder(
                                    IconCompat.createWithResource(
                                        carContext,
                                        R.drawable.ic_action_list_flat
                                    )
                                )
                                    .build()
                            )
                            .setOnClickListener {
                                isNavigationTemplate = false
                                invalidate()
                            }
                            .build())
                }
                actionStripBuilder.addAction(
                    Action.Builder()
                        .setIcon(
                            CarIcon.Builder(IconCompat.createWithResource(carContext, compassResId))
                                .build()
                        )
                        .setOnClickListener { this.compassClick() }
                        .build())
                if (app.useOpenGlRenderer()) {
                    actionStripBuilder.addAction(
                        Action.Builder()
                            .setIcon(
                                CarIcon.Builder(
                                    IconCompat.createWithResource(
                                        carContext,
                                        R.drawable.ic_action_3d
                                    )
                                )
                                    .build()
                            )
                            .setOnClickListener {
                                surfaceRenderer?.handleTilt()
                            }
                            .build()
                    )
                }
                actionStripBuilder.addAction(settingsAction)
                if (navigating) {
                    actionStripBuilder.addAction(
                        Action.Builder()
                            .setTitle(app.getString(R.string.shared_string_control_stop))
                            .setOnClickListener { listener.stopNavigation() }
                            .build())
                }
                actionStripBuilder.build()
            })

        // Set the map action strip with the pan and zoom buttons.
        navigationBuilder.setMapActionStrip(
            ActionStrip.Builder()
                .addAction(
                    Action.Builder(Action.PAN)
                        .build()
                )
                .addAction(
                    Action.Builder()
                        .setIcon(
                            CarIcon.Builder(
                                IconCompat.createWithResource(
                                    carContext,
                                    R.drawable.ic_my_location
                                )
                            )
                                .build()
                        )
                        .setOnClickListener {
                            if (!listener.requestLocationNavigation()) {
                                surfaceRenderer?.handleRecenter()
                            }
                        }
                        .build())
                .addAction(
                    Action.Builder()
                        .setIcon(
                            CarIcon.Builder(
                                IconCompat.createWithResource(
                                    carContext,
                                    R.drawable.ic_zoom_in
                                )
                            )
                                .build()
                        )
                        .setOnClickListener {
                            surfaceRenderer?.handleScale(
                                NavigationSession.INVALID_FOCAL_POINT_VAL,
                                NavigationSession.INVALID_FOCAL_POINT_VAL,
                                NavigationSession.ZOOM_IN_BUTTON_SCALE_FACTOR
                            )
                        }
                        .build())
                .addAction(
                    Action.Builder()
                        .setIcon(
                            CarIcon.Builder(
                                IconCompat.createWithResource(
                                    carContext,
                                    R.drawable.ic_zoom_out
                                )
                            )
                                .build()
                        )
                        .setOnClickListener {
                            surfaceRenderer?.handleScale(
                                NavigationSession.INVALID_FOCAL_POINT_VAL,
                                NavigationSession.INVALID_FOCAL_POINT_VAL,
                                NavigationSession.ZOOM_OUT_BUTTON_SCALE_FACTOR
                            )
                        }
                        .build())
                .build())

        // When the user enters the pan mode, remind the user that they can exit the pan mode by
        // pressing the select button again.
        navigationBuilder.setPanModeListener { isInPanMode ->
            if (isInPanMode) {
                CarToast.makeText(
                    carContext,
                    R.string.exit_pan_mode_descr,
                    CarToast.LENGTH_LONG
                ).show()
            }
            invalidate()
        }

        if (navigating) {
            destinationTravelEstimate?.also {
                navigationBuilder.setDestinationTravelEstimate(it)
            }
            if (isRerouting()) {
                navigationBuilder.setNavigationInfo(
                    RoutingInfo.Builder().setLoading(true)
                        .build())
            } else if (arrived) {
                navigationBuilder.setNavigationInfo(
                    MessageInfo.Builder(carContext.getString(R.string.arrived_at_destination))
                        .build())
            } else if (!Algorithms.isEmpty(steps)) {
                navigationBuilder.setNavigationInfo(
                    RoutingInfo.Builder().let { info ->
                        val firstStep: Step = steps!![0]
                        val currentStep = Step.Builder()
                        firstStep.cue?.also { cue ->
                            currentStep.setCue(cue.toCharSequence())
                        }
                        firstStep.maneuver?.also { maneuver ->
                            currentStep.setManeuver(maneuver)
                        }
                        firstStep.road?.also { road ->
                            currentStep.setRoad(road.toCharSequence())
                        }
                        if (shouldShowLanes) {
                            firstStep.lanes.forEach { lane ->
                                currentStep.addLane(lane)
                            }
                            firstStep.lanesImage?.also { lanesImage ->
                                currentStep.setLanesImage(lanesImage)
                            }
                        }
                        stepRemainingDistance?.also { remainingDistance ->
                            info.setCurrentStep(currentStep.build(), remainingDistance)
                            if (shouldShowNextStep && steps!!.size > 1) {
                                info.setNextStep(steps!![1])
                            }
                        }
                        junctionImage?.also { image ->
                            info.setJunctionImage(image)
                        }
                        info.build()
                    })
            }
        }
        return navigationBuilder.build()
    }

    private val surfaceRenderer: SurfaceRenderer?
        get() = app.carNavigationSession?.navigationCarSurface

    private fun compassClick() {
        app.mapViewTrackingUtilities.requestSwitchCompassToNextMode()
        invalidate()
    }

    private fun isRerouting(): Boolean {
        return rerouting || destinations == null
    }

    private fun updateCompass() {
        val settings = app.settings
        val nightMode = carContext.isDarkMode
        val compassMode = settings.compassMode
        compassResId = compassMode.getIconId(nightMode)
    }

    fun updateTrip(
        navigating: Boolean,
        rerouting: Boolean,
        arrived: Boolean,
        destinations: List<Destination>?,
        steps: List<Step>?,
        destinationTravelEstimate: TravelEstimate?,
        stepRemainingDistance: Distance?,
        shouldShowNextStep: Boolean,
        shouldShowLanes: Boolean,
        junctionImage: CarIcon?
    ) {
        this.navigating = navigating
        this.rerouting = rerouting
        this.arrived = arrived
        this.destinations = destinations
        this.steps = steps
        this.stepRemainingDistance = stepRemainingDistance
        this.destinationTravelEstimate = destinationTravelEstimate
        this.shouldShowNextStep = shouldShowNextStep
        this.shouldShowLanes = shouldShowLanes
        this.junctionImage = junctionImage
        updateNavigation()
        invalidate()
    }

    fun stopTrip() {
        navigating = false
        rerouting = false
        arrived = false
        destinations = null
        steps = null
        stepRemainingDistance = null
        destinationTravelEstimate = null
        shouldShowNextStep = false
        shouldShowLanes = false
        junctionImage = null
        updateNavigation()
    }

    private fun updateNavigation() {
        adjustMapPosition(navigating)
    }

    private fun adjustMapPosition(shiftMapIfSessionRunning: Boolean) {
        val app = app
        val session = app.carNavigationSession
        val sessionStarted = session?.hasStarted() ?: false
        val shiftMap = shiftMapIfSessionRunning && sessionStarted
        app.mapViewTrackingUtilities.mapDisplayPositionManager.setMapPositionShiftedX(shiftMap)
    }
}
