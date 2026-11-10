to = it
                            onCommand(if (it) "A1" else "A0")
                        }
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = panel
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "حالت نگهبان",
                            color = white,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "حالت مراقبت با حسگر فاصله",
                            color = purple,
                            fontSize = 11.sp
                        )
                    }

                    Switch(
                        checked = guard,
                        onCheckedChange = {
                            guard = it
                            onCommand(if (it) "G1" else "G0")
                        }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "MATRONIX  •  VERSION 1.0",
                color = purple,
                fontSize = 11.sp
            )

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        color = cyan,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun NeonDivider() {
    Spacer(Modifier.height(14.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(purple)
    )
    Spacer(Modifier.height(14.dp))
}
