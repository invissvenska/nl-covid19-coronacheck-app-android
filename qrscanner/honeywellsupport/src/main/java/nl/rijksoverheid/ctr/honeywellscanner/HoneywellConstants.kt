/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.ctr.honeywellscanner

object HoneywellConstants {

    const val HONEYWELL_INTENT_ACTION_BARCODE_DATA_FILTER_ACTION = "nl.rijksoverheid.ctr.qrscanner.ACTION"

    const val DATAWEDGE_INTENT_KEY_SYMBOLOGY = "codeId"
    const val DATAWEDGE_INTENT_KEY_DATA = "data"

    const val HONEYWELL_INTENT_ACTION_CLAIM_SCANNER = "com.honeywell.aidc.action.ACTION_CLAIM_SCANNER"
    const val HONEYWELL_INTENT_ACTION_RELEASE_SCANNER = "com.honeywell.aidc.action.ACTION_RELEASE_SCANNER"
    const val HONEYWELL_INTENT_EXTRA_SCANNER = "com.honeywell.aidc.extra.EXTRA_SCANNER"
    const val HONEYWELL_INTENT_EXTRA_PROFILE = "com.honeywell.aidc.extra.EXTRA_PROFILE"
    const val HONEYWELL_INTENT_EXTRA_PROPERTIES = "com.honeywell.aidc.extra.EXTRA_PROPERTIES"
    const val HONEYWELL_DATA_COLLECTION_SERVICE = "com.intermec.datacollectionservice"

    const val HONEYWELL_PROPERTY_DATA_PROCESSOR_PREFIX = "DPR_PREFIX"
    const val HONEYWELL_PROPERTY_DATA_PROCESSOR_SUFFIX = "DPR_SUFFIX"
    const val HONEYWELL_PROPERTY_DATA_PROCESSOR_SYMBOLOGY_PREFIX = "DPR_SYMBOLOGY_PREFIX"
    const val HONEYWELL_PROPERTY_DATA_PROCESSOR_EDIT_DATA_PLUGIN = "DPR_EDIT_DATA_PLUGIN"
    const val HONEYWELL_DATA_PROCESSOR_SYMBOLOGY_ID_NONE = "none"
    const val HONEYWELL_PROPERTY_DATA_PROCESSOR_LAUNCH_BROWSER = "DPR_LAUNCH_BROWSER"
    const val HONEYWELL_PROPERTY_DATA_PROCESSOR_DATA_INTENT = "DPR_DATA_INTENT"
    const val HONEYWELL_PROPERTY_DATA_PROCESSOR_DATA_INTENT_ACTION = "DPR_DATA_INTENT_ACTION"

    const val HONEYWELL_PROPERTY_WEDGE_ENABLED = "DPR_WEDGE"
    const val HONEYWELL_PROPERTY_QR_CODE_ENABLED = "DEC_QR_ENABLED"
}