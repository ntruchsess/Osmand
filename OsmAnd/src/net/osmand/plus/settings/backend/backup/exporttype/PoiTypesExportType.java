package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.settings.backend.ExportCategory;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;

import java.util.Collections;
import java.util.List;

class PoiTypesExportType extends AbstractExportType {

	@Override
	public int getTitleId() {
		return R.string.poi_dialog_poi_type;
	}

	@Override
	public int getIconId() {
		return R.drawable.ic_action_info_dark;
	}

	@NonNull
	@Override
	public List<?> fetchExportData(@NonNull OsmandApplication app, boolean offlineBackup) {
		boolean includeDeleted = false;
		return app.getPoiFilters().getUserDefinedPoiFilters(includeDeleted);
	}

	@NonNull
	@Override
	public ExportCategory relatedExportCategory() {
		return ExportCategory.SETTINGS;
	}

	@NonNull
	@Override
	public SettingsItemType relatedSettingsItemType() {
		return SettingsItemType.POI_UI_FILTERS;
	}

	@NonNull
	@Override
	public List<FileSubtype> relatedFileSubtypes() {
		return Collections.emptyList();
	}

	@Nullable
	@Override
	public LocalItemType relatedLocalItemType() {
		return null;
	}

	@Nullable
	@Override
	public Class<? extends OsmandPlugin> relatedPluginClass() {
		return null;
	}
}
