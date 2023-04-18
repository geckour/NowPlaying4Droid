package com.geckour.nowplaying4droid.wear

import android.content.Context
import android.graphics.Color
import androidx.wear.tiles.ActionBuilders
import androidx.wear.tiles.ColorBuilders
import androidx.wear.tiles.DeviceParametersBuilders
import androidx.wear.tiles.DimensionBuilders
import androidx.wear.tiles.LayoutElementBuilders
import androidx.wear.tiles.ModifiersBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.material.Text
import com.geckour.nowplaying4droid.R
import com.geckour.nowplaying4droid.wear.domain.model.SharingInfo
import com.geckour.nowplaying4droid.wear.ui.MainActivity
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.tiles.ExperimentalHorologistTilesApi
import com.google.android.horologist.tiles.render.SingleTileLayoutRenderer

@OptIn(ExperimentalHorologistTilesApi::class, ExperimentalHorologistApi::class)
class NP4DTileRenderer(context: Context) :
    SingleTileLayoutRenderer<SharingInfo, ByteArray?>(context) {

    companion object {

        private const val ID_ARTWORK = "id_artwork"
    }

    private val tileLayout: LayoutElementBuilders.LayoutElement =
        LayoutElementBuilders.Box.Builder()
            .setWidth(DimensionBuilders.expand())
            .setHeight(DimensionBuilders.expand())
            .addContent(
                LayoutElementBuilders.LayoutElement.Builder {
                    LayoutElementBuilders.Image.Builder()
                        .setWidth(DimensionBuilders.expand())
                        .setHeight(DimensionBuilders.expand())
                        .setResourceId(ID_ARTWORK)
                        .setModifiers(
                            ModifiersBuilders.Modifiers.Builder()
                                .setClickable(
                                    ModifiersBuilders.Clickable.Builder()
                                        .setOnClick(
                                            ActionBuilders.Action.Builder {
                                                ActionBuilders.LaunchAction.Builder()
                                                    .setAndroidActivity(
                                                        ActionBuilders.AndroidActivity.Builder()
                                                            .setPackageName("com.geckour.nowplaying4droid")
                                                            .setClassName("com.geckour.nowplaying4droid.wear.ui.MainActivity")
                                                            .addKeyToExtraMapping(
                                                                MainActivity.ARG_AUTO_SHARE,
                                                                ActionBuilders.AndroidExtra.Builder {
                                                                    ActionBuilders.AndroidBooleanExtra.Builder()
                                                                        .setValue(true)
                                                                        .build()
                                                                }.build()
                                                            )
                                                            .build()
                                                    )
                                                    .build()
                                            }.build()
                                        )
                                        .build()
                                )
                                .build()
                        )
                        .build()
                }.build()
            )
            .addContent(
                LayoutElementBuilders.Box.Builder()
                    .setWidth(DimensionBuilders.expand())
                    .setHeight(DimensionBuilders.expand())
                    .setModifiers(
                        ModifiersBuilders.Modifiers.Builder()
                            .setBackground(
                                ModifiersBuilders.Background.Builder()
                                    .setColor(
                                        ColorBuilders.ColorProp.Builder()
                                            .setArgb(context.getColor(R.color.colorMaskIcon))
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .addContent(
                Text.Builder(context, context.getString(R.string.share_auto_twitter))
                    .setColor(
                        ColorBuilders.ColorProp.Builder()
                            .setArgb(Color.WHITE)
                            .build()
                    )
                    .setOverflow(LayoutElementBuilders.TEXT_OVERFLOW_ELLIPSIZE_END)
                    .setTypography(androidx.wear.tiles.material.Typography.TYPOGRAPHY_BODY1)
                    .setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD)
                    .build()
            )
            .build()

    override fun renderTile(
        state: SharingInfo,
        deviceParameters: DeviceParametersBuilders.DeviceParameters
    ): LayoutElementBuilders.LayoutElement = tileLayout

    override fun ResourceBuilders.Resources.Builder.produceRequestedResources(
        resourceState: ByteArray?,
        deviceParameters: DeviceParametersBuilders.DeviceParameters,
        resourceIds: MutableList<String>
    ) {
        addIdToImageMapping(
            ID_ARTWORK,
            ResourceBuilders.ImageResource.Builder()
                .setAndroidResourceByResId(
                    ResourceBuilders.AndroidImageResourceByResId.Builder()
                        .setResourceId(R.mipmap.ic_launcher)
                        .build()
                )
                .build()
        )
    }
}