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

class QuickActionsExportType extends AbstractExportType {

	@Override
	public int getTitleId() {
		return R.string.configure_screen_quick_action;
	}

	@Override
	public int getIconId() {
		return R.drawable.ic_quick_action;
	}

	@NonNull
	@Override
	public List<?> fetchExportData(@NonNull OsmandApplication app, boolean offlineBackup) {
		return app.getQuickActionRegistry().getQuickActions();
	}

	@NonNull
	@Override
	public ExportCategory relatedExportCategory() {
		return ExportCategory.SETTINGS;
	}

	@NonNull
	@Override
	public SettingsItemType relatedSettingsItemType() {
		return SettingsItemType.QUICK_ACTIONS;
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
