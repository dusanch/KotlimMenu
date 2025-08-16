package com.dct.qr.ui.theme // Alebo váš balíček

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes // Toto je import triedy, nie objektu
import androidx.compose.ui.unit.dp

val AppShapes = Shapes( // <--- TOTO JE OBJEKT (INŠTANCIA TRIEDY SHAPES)
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp), // Alebo vaše hodnoty
    extraLarge = RoundedCornerShape(16.dp) // Alebo vaše hodnoty
)