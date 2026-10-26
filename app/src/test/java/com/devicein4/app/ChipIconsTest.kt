package com.devicein4.app

import org.junit.Assert.assertEquals
import org.junit.Test

class ChipIconsTest {
    @Test fun recognizesChipAndGpuAliases() {
        assertEquals("brand_qualcomm", IconAssets.chip("Adreno (TM) 740"))
        assertEquals("brand_mediatek", IconAssets.chip("MT6989"))
        assertEquals("brand_arm", IconAssets.chip("Mali-G715"))
        assertEquals("brand_imagination", IconAssets.chip("PowerVR Rogue"))
    }

    @Test fun identifiesManufacturerBeforeGenericArmArchitecture() {
        assertEquals("brand_samsung", IconAssets.chip("ARMv8 Processor samsungexynos"))
        assertEquals("brand_google", IconAssets.chip("Tensor G4 ARMv8"))
        assertEquals("brand_amd", IconAssets.chip("Samsung Xclipse 940"))
    }

    @Test fun unknownChipKeepsDefaultIcon() {
        assertEquals("", IconAssets.chip("Không được Android công khai"))
        assertEquals("", IconAssets.chip(""))
    }
}
