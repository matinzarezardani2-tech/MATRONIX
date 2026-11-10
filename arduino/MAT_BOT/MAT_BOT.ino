/*
  MAT BOT - starter firmware for Arduino UNO R3
  Hardware assumptions:
  - HC-05 Bluetooth module on SoftwareSerial pins 10(RX), 11(TX)
  - TB6612FNG motor driver
  - SSD1306 128x64 I2C OLED at address 0x3C
  - Optional HC-SR04: TRIG=A0, ECHO=A1

  Install libraries from Arduino Library Manager:
  Adafruit GFX Library
  Adafruit SSD1306

  IMPORTANT: Verify your exact OLED controller/interface and motor driver pinout
  before powering the circuit. HC-05 TX -> Arduino D10; Arduino D11 -> HC-05 RX
  through a suitable voltage divider (UNO TX is 5V).
*/
#include <SoftwareSerial.h>
#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>

#define SCREEN_WIDTH 128
#define SCREEN_HEIGHT 64
Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, -1);

SoftwareSerial bt(10, 11); // Arduino RX, TX

// TB6612FNG pins
const int PWMA = 5, AIN1 = 7, AIN2 = 8;
const int PWMB = 6, BIN1 = 4, BIN2 = 3;
const int STBY = 2;

// Optional ultrasonic sensor
const int TRIG_PIN = A0, ECHO_PIN = A1;
bool guardMode = false, autoMode = false;
int motorSpeed = 150;
String emotion = "E1";
String inputLine;

void setMotor(int pwmPin, int in1, int in2, int speedValue) {
  int s = constrain(speedValue, -255, 255);
  digitalWrite(in1, s >= 0 ? HIGH : LOW);
  digitalWrite(in2, s >= 0 ? LOW : HIGH);
  analogWrite(pwmPin, abs(s));
}
void stopMotors() {
  analogWrite(PWMA, 0); analogWrite(PWMB, 0);
}
void drive(char c) {
  digitalWrite(STBY, HIGH);
  switch (c) {
    case 'F': setMotor(PWMA, AIN1, AIN2, motorSpeed); setMotor(PWMB, BIN1, BIN2, motorSpeed); break;
    case 'B': setMotor(PWMA, AIN1, AIN2, -motorSpeed); setMotor(PWMB, BIN1, BIN2, -motorSpeed); break;
    case 'L': setMotor(PWMA, AIN1, AIN2, -motorSpeed); setMotor(PWMB, BIN1, BIN2, motorSpeed); break;
    case 'R': setMotor(PWMA, AIN1, AIN2, motorSpeed); setMotor(PWMB, BIN1, BIN2, -motorSpeed); break;
    default: stopMotors(); break;
  }
}
void drawFace() {
  display.clearDisplay();
  display.setTextColor(SSD1306_WHITE);
  display.setTextSize(1);
  display.setCursor(0, 0); display.print("MAT BOT  ");
  display.print(emotion);
  // Simple placeholder face; customize expressions after confirming OLED.
  display.fillRoundRect(24, 20, 28, 24, 8, SSD1306_WHITE);
  display.fillRoundRect(76, 20, 28, 24, 8, SSD1306_WHITE);
  display.fillCircle(38, 32, 5, SSD1306_BLACK);
  display.fillCircle(90, 32, 5, SSD1306_BLACK);
  display.drawLine(48, 53, 80, 53, SSD1306_WHITE);
  display.display();
}
long distanceCm() {
  digitalWrite(TRIG_PIN, LOW); delayMicroseconds(2);
  digitalWrite(TRIG_PIN, HIGH); delayMicroseconds(10);
  digitalWrite(TRIG_PIN, LOW);
  long duration = pulseIn(ECHO_PIN, HIGH, 25000);
  if (duration == 0) return 999;
  return duration / 58;
}
void handleCommand(String cmd) {
  cmd.trim();
  if (cmd == "F" || cmd == "B" || cmd == "L" || cmd == "R" || cmd == "X") {
    drive(cmd[0]); return;
  }
  if (cmd.startsWith("V:")) {
    motorSpeed = constrain(cmd.substring(2).toInt(), 0, 255); return;
  }
  if (cmd.startsWith("E")) { emotion = cmd; drawFace(); return; }
  if (cmd == "G1") { guardMode = true; return; }
  if (cmd == "G0") { guardMode = false; return; }
  if (cmd == "A1") { autoMode = true; return; }
  if (cmd == "A0") { autoMode = false; return; }
  if (cmd == "SOUND1") {
    // Sound hardware is not connected in this starter version.
    bt.println("SOUND_NOT_CONFIGURED");
    return;
  }
}
void setup() {
  pinMode(PWMA, OUTPUT); pinMode(AIN1, OUTPUT); pinMode(AIN2, OUTPUT);
  pinMode(PWMB, OUTPUT); pinMode(BIN1, OUTPUT); pinMode(BIN2, OUTPUT);
  pinMode(STBY, OUTPUT); digitalWrite(STBY, HIGH);
  pinMode(TRIG_PIN, OUTPUT); pinMode(ECHO_PIN, INPUT);
  stopMotors();
  bt.begin(9600);
  Serial.begin(9600);
  if (display.begin(SSD1306_SWITCHCAPVCC, 0x3C)) drawFace();
}
void loop() {
  while (bt.available()) {
    char c = bt.read();
    if (c == '\n' || c == '\r') {
      if (inputLine.length()) handleCommand(inputLine);
      inputLine = "";
    } else if (inputLine.length() < 32) inputLine += c;
  }

  if (guardMode) {
    // Guard mode only detects proximity; it does not drive the robot autonomously.
    if (distanceCm() < 20) {
      stopMotors();
      bt.println("OBSTACLE");
    }
  }

  if (autoMode) {
    // Placeholder: automatic personality/face animation can be added here.
    static unsigned long lastChange = 0;
    if (millis() - lastChange > 5000) {
      lastChange = millis();
      emotion = (emotion == "E1") ? "E7" : "E1";
      drawFace();
    }
  }
}
