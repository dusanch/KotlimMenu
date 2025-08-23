package com.dct.qr.ui.scanresult

import androidx.compose.runtime.remember
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dct.qr.R
import com.google.mlkit.vision.barcode.common.Barcode

// Pomocná dátová trieda pre tlačidlá akcie
data class ActionButtonData(
    val textResId: Int,
    val icon: ImageVector,
    val action: (Context, String) -> Unit
)

@Composable
fun ScanResultScreen(
    scannedValue: String,
    barcodeType: Int, // Typ z ML Kit Barcode.TYPE_*
    onNavigateBack: () -> Unit // Pre tlačidlo späť, ak ho chcete explicitne
) {
    val context = LocalContext.current

    val actionButtons = remember(barcodeType, scannedValue) {
        getActionButtonsForType(barcodeType, scannedValue, context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.scan_result_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            SelectionContainer { // Umožní kopírovanie textu podržaním
                Text(
                    text = scannedValue,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        actionButtons.forEach { buttonData ->
            ActionButton(
                text = stringResource(buttonData.textResId),
                icon = buttonData.icon,
                onClick = { buttonData.action(context, scannedValue) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Tlačidlo Späť, ak ho potrebujete explicitne (TopAppBar by to mal tiež riešiť)
        // Spacer(modifier = Modifier.weight(1f))
        // Button(onClick = onNavigateBack) {
        //     Text("Späť na skenovanie")
        // }
    }
}

@Composable
private fun ActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Icon(imageVector = icon, contentDescription = text, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text)
    }
}


fun getActionButtonsForType(barcodeType: Int, value: String, context: Context): List<ActionButtonData> {
    val buttons = mutableListOf<ActionButtonData>()

    when (barcodeType) {
        Barcode.TYPE_EMAIL -> {
            buttons.add(ActionButtonData(R.string.action_send_email, Icons.Filled.Email) { ctx, email ->
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:$email")
                }
                try {
                    ctx.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(ctx, ctx.getString(R.string.no_email_app_found), Toast.LENGTH_SHORT).show()
                }
            })
            buttons.add(ActionButtonData(R.string.action_add_contact, Icons.Filled.PersonAdd) { ctx, _ ->
                val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
                    type = ContactsContract.RawContacts.CONTENT_TYPE
                    putExtra(ContactsContract.Intents.Insert.EMAIL, value)
                }
                try {
                    ctx.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(ctx, "Chyba pri otváraní kontaktov.", Toast.LENGTH_SHORT).show()
                }
            })
        }
        Barcode.TYPE_URL -> {
            buttons.add(ActionButtonData(R.string.action_open_link, Icons.Filled.Search/*Môžete použiť inú ikonu*/) { ctx, url ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                try {
                    ctx.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(ctx, ctx.getString(R.string.no_browser_found), Toast.LENGTH_SHORT).show()
                }
            })
        }
        Barcode.TYPE_PHONE -> {
            buttons.add(ActionButtonData(R.string.action_call_phone, Icons.Filled.Email /* Nahraďte ikonou telefónu */) { ctx, phone ->
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                try {
                    ctx.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(ctx, "Chyba pri otváraní vytáčania.", Toast.LENGTH_SHORT).show()
                }
            })
            buttons.add(ActionButtonData(R.string.action_add_contact, Icons.Filled.PersonAdd) { ctx, _ ->
                val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
                    type = ContactsContract.RawContacts.CONTENT_TYPE
                    putExtra(ContactsContract.Intents.Insert.PHONE, value)
                }
                try {
                    ctx.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(ctx, "Chyba pri otváraní kontaktov.", Toast.LENGTH_SHORT).show()
                }
            })
        }
        Barcode.TYPE_SMS -> {
            buttons.add(ActionButtonData(R.string.action_send_sms, Icons.Filled.Email /* Nahraďte ikonou SMS */) { ctx, _ ->
                // Pre SMS potrebujete parsovať číslo a správu, ak sú v štruktúrovanom formáte
                // napr. "smsto:1234567890:Hello World"
                val uri = if (value.startsWith("smsto:")) Uri.parse(value) else Uri.parse("smsto:${extractPhoneNumberFromSmsString(value)}")
                val intent = Intent(Intent.ACTION_SENDTO, uri)
                // Ak je v `value` aj správa, môžete ju pridať do extra
                // intent.putExtra("sms_body", extractMessageFromSmsString(value));
                try {
                    ctx.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(ctx, "Chyba pri otváraní SMS aplikácie.", Toast.LENGTH_SHORT).show()
                }
            })
        }
        Barcode.TYPE_WIFI -> {
            buttons.add(ActionButtonData(R.string.action_connect_wifi, Icons.Filled.Email /* Nahraďte ikonou WiFi */) { ctx, _ ->
                // Pripojenie k WiFi je zložitejšie a vyžaduje špeciálne povolenia a API od Android 10 (Q)
                // Pre jednoduchosť môžete zobraziť detail siete
                Toast.makeText(ctx, "Detail siete WiFi (implementácia TODO):\n$value", Toast.LENGTH_LONG).show()
            })
        }
        Barcode.TYPE_GEO -> { // Geo súradnice
            buttons.add(ActionButtonData(R.string.action_show_on_map, Icons.Filled.Email /* Ikona mapy */) { ctx, geo ->
                // geo:latitude,longitude alebo geo:latitude,longitude?q=query
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(geo))
                try {
                    ctx.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(ctx, "Nepodarilo sa otvoriť mapu.", Toast.LENGTH_SHORT).show()
                }
            })
        }
        Barcode.TYPE_CONTACT_INFO -> {
            buttons.add(ActionButtonData(R.string.action_add_contact, Icons.Filled.PersonAdd) { ctx, _ ->
                // Toto je komplexnejšie, pretože Barcode.TYPE_CONTACT_INFO môže obsahovať veľa polí.
                // ML Kit poskytuje štruktúrované dáta. Pre jednoduchosť tu len zobrazíme:
                Toast.makeText(ctx, "Pridať kontakt (implementácia TODO):\n$value", Toast.LENGTH_LONG).show()
                // V reálnej aplikácii by ste parsovali Barcode.contactInfo a vyplnili Intent pre kontakty
            })
        }
        // ... ďalšie typy ako Barcode.TYPE_CALENDAR_EVENT, Barcode.TYPE_DRIVER_LICENSE ...

        else -> { // Predvolené pre TYPE_TEXT a neznáme typy
            buttons.add(ActionButtonData(R.string.action_search_web, Icons.Filled.Search) { ctx, text ->
                val intent = Intent(Intent.ACTION_WEB_SEARCH)
                intent.putExtra(android.app.SearchManager.QUERY, text)
                try {
                    ctx.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(ctx, "Nepodarilo sa spustiť vyhľadávanie.", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    // Spoločné tlačidlá pre väčšinu typov
    buttons.add(ActionButtonData(R.string.action_share, Icons.Filled.Share) { ctx, text ->
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        ctx.startActivity(Intent.createChooser(intent, ctx.getString(R.string.share_via)))
    })
    buttons.add(ActionButtonData(R.string.action_copy, Icons.Filled.ContentCopy) { ctx, text ->
        val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("QR_Code_Content", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(ctx, ctx.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
    })

    return buttons
}

// Pomocné funkcie pre parsovanie SMS (veľmi zjednodušené)
fun extractPhoneNumberFromSmsString(smsString: String): String {
    return if (smsString.contains(":")) smsString.substringBefore(":") else smsString
}


