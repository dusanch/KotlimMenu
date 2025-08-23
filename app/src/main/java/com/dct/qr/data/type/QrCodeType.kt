package com.dct.qr.data.type

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TextFields // Obyčajný text
import androidx.compose.material.icons.filled.Link      // URL
import androidx.compose.material.icons.filled.Wifi       // WiFi
import androidx.compose.material.icons.filled.Email      // Email adresa
import androidx.compose.material.icons.filled.AlternateEmail // Email správa (mailto: s detailmi)
import androidx.compose.material.icons.filled.Phone      // Telefónne číslo
import androidx.compose.material.icons.filled.Sms        // SMS správa
import androidx.compose.material.icons.filled.LocationOn  // Geolokácia
import androidx.compose.material.icons.filled.Event      // Udalosť v kalendári (VEVENT)
import androidx.compose.material.icons.filled.Person     // Kontakt (vCard)
import androidx.compose.material.icons.filled.ContactMail // Kontakt (MeCard - jednoduchšia alternatíva k vCard)
import androidx.compose.material.icons.filled.Book       // ISBN (kniha)
import androidx.compose.material.icons.filled.QrCodeScanner // Produktový kód ( všeobecný, napr. EAN)
import androidx.compose.material.icons.filled.AppSettingsAlt // App Link / Deep Link / Market Link
import androidx.compose.material.icons.filled.CreditCard // Platobné údaje (veľmi všeobecne, konkrétne formáty sú komplexné)
import androidx.compose.material.icons.filled.Key // Kľúče, napr. pre 2FA (TOTP)
import androidx.compose.material.icons.filled.Share // Sociálne siete
import androidx.compose.material.icons.filled.Style // EPC QR Code (SEPA platby)
import androidx.compose.material.icons.automirrored.filled.Send // MMS
// ... pridajte ďalšie potrebné importy ikon z androidx.compose.material.icons.material.filled alebo extended

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dct.qr.R // Nahraďte com.dct.qr vaším R súborom ak je iný


enum class QrCodeType(
    val displayNameResourceId: Int,
    val icon: ImageVector,
    val dataPrefix: String? = null,
    val dataFormatHintResourceId: Int? = null,
    val requiresComplexInput: Boolean = false // Flag pre typy, ktoré potrebujú špecializovaný formulár
) {
    TEXT(
        displayNameResourceId = R.string.qr_type_text,
        icon = Icons.Filled.TextFields,
        dataFormatHintResourceId = R.string.qr_format_hint_text
    ),
    URL(
        displayNameResourceId = R.string.qr_type_url,
        icon = Icons.Filled.Link,
        dataPrefix = "https://",
        dataFormatHintResourceId = R.string.qr_format_hint_url
    ),
    WIFI(
        displayNameResourceId = R.string.qr_type_wifi,
        icon = Icons.Filled.Wifi,
        dataPrefix = "WIFI:",
        dataFormatHintResourceId = R.string.qr_format_hint_wifi,
        requiresComplexInput = true
    ),
    EMAIL_ADDRESS( // Len samotná adresa
        displayNameResourceId = R.string.qr_type_email_address,
        icon = Icons.Filled.Email,
        dataPrefix = "mailto:", // Pre jednoduché otvorenie emailového klienta s adresou
        dataFormatHintResourceId = R.string.qr_format_hint_email_address
    ),
    EMAIL_MESSAGE( // Predvyplnený email
        displayNameResourceId = R.string.qr_type_email_message,
        icon = Icons.Filled.AlternateEmail,
        dataPrefix = "mailto:", // mailto:address?subject=SUBJECT&body=BODY&cc=CC&bcc=BCC
        dataFormatHintResourceId = R.string.qr_format_hint_email_message,
        requiresComplexInput = true
    ),
    PHONE(
        displayNameResourceId = R.string.qr_type_phone,
        icon = Icons.Filled.Phone,
        dataPrefix = "tel:",
        dataFormatHintResourceId = R.string.qr_format_hint_phone
    ),
    SMS(
        displayNameResourceId = R.string.qr_type_sms,
        icon = Icons.Filled.Sms,
        dataPrefix = "smsto:", // smsto:phoneNumber:messageText
        dataFormatHintResourceId = R.string.qr_format_hint_sms,
        requiresComplexInput = true // Ak chcete oddeliť číslo a text správy
    ),
    MMS( // Menej časté, ale možné
        displayNameResourceId = R.string.qr_type_mms,
        icon = Icons.AutoMirrored.Filled.Send,
        dataPrefix = "mmsto:", // mmsto:phoneNumber:subject:body (podpora sa môže líšiť)
        dataFormatHintResourceId = R.string.qr_format_hint_mms,
        requiresComplexInput = true
    ),
    GEOLOCATION(
        displayNameResourceId = R.string.qr_type_geolocation,
        icon = Icons.Filled.LocationOn,
        dataPrefix = "geo:", // geo:latitude,longitude?q=query (alebo geo:latitude,longitude,altitude)
        dataFormatHintResourceId = R.string.qr_format_hint_geolocation,
        requiresComplexInput = true // Pre oddelené zadávanie lat/lon
    ),
    CALENDAR_EVENT_VEVENT(
        displayNameResourceId = R.string.qr_type_calendar_event_vevent,
        icon = Icons.Filled.Event,
        dataPrefix = "BEGIN:VEVENT\n",
        dataFormatHintResourceId = R.string.qr_format_hint_calendar_event_vevent,
        requiresComplexInput = true
    ),
    CONTACT_VCARD(
        displayNameResourceId = R.string.qr_type_contact_vcard,
        icon = Icons.Filled.Person,
        dataPrefix = "BEGIN:VCARD\nVERSION:3.0\n",
        dataFormatHintResourceId = R.string.qr_format_hint_contact_vcard,
        requiresComplexInput = true
    ),
    CONTACT_MECARD( // Jednoduchšia alternatíva k vCard
        displayNameResourceId = R.string.qr_type_contact_mecard,
        icon = Icons.Filled.ContactMail,
        dataPrefix = "MECARD:", // MECARD:N:Last,First;TEL:123;EMAIL:a@b.com;URL:http://...;;
        dataFormatHintResourceId = R.string.qr_format_hint_contact_mecard,
        requiresComplexInput = true
    ),
    APP_STORE_LINK( // Odkaz na obchod s aplikáciami (Google Play, Apple App Store)
        displayNameResourceId = R.string.qr_type_app_store_link,
        icon = Icons.Filled.AppSettingsAlt, // alebo špecifická ikona pre obchod
        // Napr. market://details?id=com.example.app alebo http://itunes.apple.com/...
        dataFormatHintResourceId = R.string.qr_format_hint_app_store_link
    ),
    DEEP_LINK( // Odkaz do špecifickej časti aplikácie
        displayNameResourceId = R.string.qr_type_deep_link,
        icon = Icons.Filled.AppSettingsAlt, // Ikona môže byť rovnaká
        // Napr. yourappscheme://path/to/content
        dataFormatHintResourceId = R.string.qr_format_hint_deep_link
    ),
    ISBN( // International Standard Book Number
        displayNameResourceId = R.string.qr_type_isbn,
        icon = Icons.Filled.Book,
        dataPrefix = "ISBN:", // Nie je to štandardný URI prefix, ale môže pomôcť identifikovať typ
        dataFormatHintResourceId = R.string.qr_format_hint_isbn
    ),
    PRODUCT_CODE_EAN_UPC( // EAN, UPC kódy
        displayNameResourceId = R.string.qr_type_product_code,
        icon = Icons.Filled.QrCodeScanner, // Všeobecná ikona
        dataFormatHintResourceId = R.string.qr_format_hint_product_code
    ),
    CRYPTOCURRENCY_ADDRESS( // Napr. Bitcoin, Ethereum adresa
        displayNameResourceId = R.string.qr_type_crypto_address,
        icon = Icons.Filled.CreditCard, // Zvážte špecifickejšiu ikonu
        // Formáty sa líšia, napr. bitcoin:address?amount=0.1&label=MyLabel
        dataFormatHintResourceId = R.string.qr_format_hint_crypto_address,
        requiresComplexInput = true
    ),
    EPC_PAYMENT_SEPA( // European Payments Council QR Code pre SEPA platby
        displayNameResourceId = R.string.qr_type_epc_payment,
        icon = Icons.Filled.Style, // Alebo vhodnejšia ikona pre platby
        // Špecifický formát začínajúci BCD\n002...
        dataFormatHintResourceId = R.string.qr_format_hint_epc_payment,
        requiresComplexInput = true
    ),
    TOTP_AUTHENTICATOR( // Pre nastavenie 2FA v aplikáciách ako Google Authenticator
        displayNameResourceId = R.string.qr_type_totp_authenticator,
        icon = Icons.Filled.Key,
        dataPrefix = "otpauth://totp/", // otpauth://totp/LABEL?secret=SECRET&issuer=ISSUER
        dataFormatHintResourceId = R.string.qr_format_hint_totp_authenticator,
        requiresComplexInput = true
    ),
    SOCIAL_MEDIA_PROFILE_GENERIC( // Všeobecný odkaz na sociálnu sieť
        displayNameResourceId = R.string.qr_type_social_media_profile,
        icon = Icons.Filled.Share,
        dataFormatHintResourceId = R.string.qr_format_hint_social_media_profile
        // Tu by používateľ zadal plnú URL profilu
    );
    // Ďalšie možné typy:
    // - WhatsApp odkaz (https://wa.me/<number>)
    // - FaceTime odkaz (facetime://<appleid_or_phonenumber>)
    // - PayPal.Me odkaz
    // - Špecifické formáty pre konkrétne aplikácie alebo služby

    @Composable
    fun getDisplayName(): String {
        return stringResource(id = displayNameResourceId)
    }

    @Composable
    fun getDataFormatHint(): String? {
        return dataFormatHintResourceId?.let { stringResource(id = it) }
    }

    // Pomocná funkcia na vytvorenie predvyplneného reťazca pre UI (ak má prefix)
    fun getPrefilledInput(): String {
        return dataPrefix ?: ""
    }

    // Funkcia na sformátovanie dát (zatiaľ len pre jednoduché prefixy, pre komplexné typy budete potrebovať viac)
    fun formatData(userInput: String): String {
        if (dataPrefix == null) return userInput // Ak nie je prefix, vráti pôvodný vstup

        // Jednoduchá logika pre prefixy - ak užívateľ už zadal niečo, čo vyzerá ako prefix, nepridávaj znova
        // Toto je veľmi základné, pre URL by ste mohli chcieť sofistikovanejšiu kontrolu
        val commonSchemes = listOf("http://", "https://", "ftp://", "mailto:", "tel:", "smsto:", "geo:", "WIFI:")
        if (commonSchemes.any { userInput.lowercase().startsWith(it) }) {
            return userInput
        }
        return dataPrefix + userInput
    }
}

