#include "FastLED.h"
//#include "lights.h"

#define maxNUM_LEDS 17
#define NUM_LEDS 18

int numLeds[3] = {NUM_LEDS , NUM_LEDS, NUM_LEDS };

CRGB led0[NUM_LEDS];
CRGB led1[NUM_LEDS];
CRGB* leds[2];

#define PIN1 8
#define PIN2 2


bool blueteam = false;

void setPixel( int Pixel, byte red, byte green, byte blue, int strip = 0);
void setAll(byte red, byte green, byte blue, int strip = 0);

String inputString = "";         // a String to hold incoming data
String lastInput = "";
String lastlastInput = "";
boolean stringComplete = false;  // whether the string is complete


void setup() {
  leds[0] = led0;
  leds[1] = led1;
  FastLED.addLeds<WS2811, PIN1, BRG>(led0, numLeds[0]).setCorrection( TypicalLEDStrip );
  FastLED.addLeds<WS2811, PIN2, BRG>(led1, numLeds[1]).setCorrection( TypicalLEDStrip );
  Serial.begin(9600);
}

void loop() {
  //Fire(55,120,15);
  //plzstop();
  //go();
  //setEverything(0,0,0);
  //showStrip();
  ReadCommands();
 //test();

}



void serialEvent(){
    while (Serial.available() > 0) {
    char inChar = (char)Serial.read();
    inputString += inChar;
    if (inChar == '\n' || inChar == ' ') {
      stringComplete = true;
      lastlastInput = lastInput;
      lastInput = inputString;
      inputString = "";
      lastInput = lastInput.substring(0,lastInput.length()-1);
      //Serial.print(("aaa"+lastInput+"aaa"));
    }
  }
}

void plzstop(){
  setEverything(0x7A,0x06,0xBC);
}

void test(){
  for( int i = 0; i < NUM_LEDS; i++) {
    //for(int r = 0; r<1;r++){
    if (isInterrupted()){
      return;
    }
    setEverything(0,0,0);
    showStrip();
    delay(400);
    if (isInterrupted()){
      return;
    }
    setPixeldouble(i, 100, 0, 0);
    showStrip();
    delay(400);
    if (isInterrupted()){
      return;
    }
   // }
  }
}
void test2(){
  setAll(100, 0, 0, 0);
  setAll(100, 0, 0, 1);
  showStrip();
}

void go(){
  setEverything(0,125,0);
}

void bounce(){
  if (blueteam){
    RightToLeft(0, 0, 0xff, 3, 40, 40);
    LeftToRight(0, 0, 0xff, 3, 40, 40);
  }
  else{
    RightToLeft(0xff, 0, 0, 3, 40, 40);
    LeftToRight(0xff, 0, 0, 3, 40, 40);
  }

}

void ReadCommands() {
  if (stringComplete) {
    stringComplete = false;
  }

  if (lastInput == "stop"){
    plzstop();
  }
  else if (lastInput == "blue" || lastInput == "b"){
    blueteam = true;
    lastInput = lastlastInput;
  }
  else if (lastInput == "red" || lastInput == "r"){
    blueteam = false;
    lastInput = lastlastInput;
  }
  else if (lastInput == "clear"){
    setEverything(0,0,0);
  }
  else if (lastInput == "test"){
    test();
  }
  else if (lastInput == "go"){
    go();
  }
  else if (lastInput == "fire"){
    //Fire(55,120,25,1);

    Fire(40,120,25);
    //Fire(55,120,25,0);
  }
  else if (lastInput == "the"){
    if (blueteam){
    theaterChase(0, 0, 255,50);
    }else{
      theaterChase(255, 0, 0,50);
    }
  }
  else if (lastInput == "rainbow"){
    theaterChaseRainbow(50);
  }
  else {
    bounce();
  }
}


void Fire(int Cooling, int Sparking, int SpeedDelay) {
  int fakenum = NUM_LEDS;
  static byte heat[NUM_LEDS];
  int cooldown;

  // Step 1.  Cool down every cell a little
  for( int i = 0; i < fakenum; i++) {
    cooldown = random(0, ((Cooling * 10) / fakenum) + 2);

    if(cooldown>heat[i]) {
      heat[i]=0;
    } else {
      heat[i]=heat[i]-cooldown;
    }
  }

  // Step 2.  Heat from each cell drifts 'up' and diffuses a little
  for( int k= (fakenum) - 1; k >= 2; k--) {
    heat[k] = (heat[k - 1] + heat[k - 2] + heat[k - 2]) / 3;
  }

  // Step 3.  Randomly ignite new 'sparks' near the bottom
  if( random(255) < Sparking ) {
    int y = random(7);
    heat[y] = heat[y] + random(160,255);
    //heat[y] = random(160,255);
  }

  // Step 4.  Convert heat to LED colors
  for( int j = 0; j < fakenum; j++) {
    setPixelHeatColor(j, heat[j]);
  }

  showStrip();
  delay(SpeedDelay);
}

void setPixelHeatColor (int Pixel, byte temperature) {
  // Scale 'heat' down from 0-255 to 0-191
  byte t192 = round((temperature/255.0)*191);

  // calculate ramp up from
  byte heatramp = t192 & 0x3F; // 0..63
  heatramp <<= 2; // scale up to 0..252

  // figure out which third of the spectrum we're in:
  if (blueteam){
  if( t192 > 0x80) {                     // hottest
    setPixeldouble(Pixel, heatramp, 255, 255);
  } else if( t192 > 0x40 ) {             // middle
    setPixeldouble(Pixel, 255, heatramp, 255);
  } else {                               // coolest
    setPixeldouble(Pixel, 0, 0, heatramp);
  }
  }
  else {
  if( t192 > 0x80) {                     // hottest
    setPixeldouble(Pixel, 255, 255, heatramp);
  } else if( t192 > 0x40 ) {             // middle
    setPixeldouble(Pixel, 255, heatramp, 0);
  } else {                               // coolest
    setPixeldouble(Pixel, heatramp, 0, 0);
  }
  }

}

void theaterChase(byte red, byte green, byte blue, int SpeedDelay) {
  for (int j=0; j<10; j++) {  //do 10 cycles of chasing
    if (isInterrupted()){
      return;
    }
    for (int q=0; q < 3; q++) {
      if (isInterrupted()){
      return;
    }
      for (int i=0; i < NUM_LEDS; i=i+3) {
        if (isInterrupted()){
      return;
    }
        setPixeldoubleopposites(i+q, red, green, blue);
      }
      showStrip();

      delay(SpeedDelay);

      for (int i=0; i < NUM_LEDS; i=i+3) {
        if (isInterrupted()){
      return;
    }
        setPixeldoubleopposites(i+q, 0,0,0);
      }
    }
  }
}

void theaterChaseRainbow(int SpeedDelay) {
  byte *c;

  for (int j=0; j < 256; j++) {     // cycle all 256 colors in the wheel
    for (int q=0; q < 3; q++) {
        for (int i=0; i < NUM_LEDS; i=i+3) {
          if (isInterrupted()){
            return;
          }
          c = Wheel( (i+j) % 255);
          setPixeldoubleopposites(i+q, *c, *(c+1), *(c+2));    //turn every third pixel on
        }
        showStrip();

        delay(SpeedDelay);

        for (int i=0; i < NUM_LEDS; i=i+3) {
          if (isInterrupted()){
            return;
          }
          setPixeldoubleopposites(i+q, 0,0,0);        //turn every third pixel off
        }
    }
  }
}

byte * Wheel(byte WheelPos) {
  static byte c[3];

  if(WheelPos < 85) {
   c[0]=WheelPos * 3;
   c[1]=255 - WheelPos * 3;
   c[2]=0;
  } else if(WheelPos < 170) {
   WheelPos -= 85;
   c[0]=255 - WheelPos * 3;
   c[1]=0;
   c[2]=WheelPos * 3;
  } else {
   WheelPos -= 170;
   c[0]=0;
   c[1]=WheelPos * 3;
   c[2]=255 - WheelPos * 3;
  }

  return c;
}


void LeftToRight(byte red, byte green, byte blue, int EyeSize, int SpeedDelay, int ReturnDelay) {
  for(int i = 0; i < NUM_LEDS-EyeSize-2; i++) {
if (isInterrupted()){
      return;
    }
    setEverything(0,0,0);
    setPixeldouble(i, red/10, green/10, blue/10);
    for(int j = 1; j <= EyeSize; j++) {
      if (isInterrupted()){
      return;
    }
      setPixeldouble(i+j, red, green, blue);
    }
    setPixeldouble(i+EyeSize+1, red/10, green/10, blue/10);
    showStrip();

    delay(SpeedDelay);
  }
  delay(ReturnDelay);
}

void RightToLeft(byte red, byte green, byte blue, int EyeSize, int SpeedDelay, int ReturnDelay) {
  for(int i = NUM_LEDS-EyeSize-2; i > 0; i--) {
    if (isInterrupted()){
      return;
    }
    setEverything(0,0,0);
    setPixeldouble(i, red/10, green/10, blue/10);
    for(int j = 1; j <= EyeSize; j++) {
      if (isInterrupted()){
      return;
    }
      setPixeldouble(i+j, red, green, blue);
    }
    setPixeldouble(i+EyeSize+1, red/10, green/10, blue/10);
    showStrip();

    delay(SpeedDelay);
  }
  delay(ReturnDelay);
}

void setPixel(int Pixel, byte red, byte green, byte blue, int strip) {
 #ifndef ADAFRUIT_NEOPIXEL_H
   // FastLED
   leds[strip][Pixel].r = red;
   leds[strip][Pixel].g = green;
   leds[strip][Pixel].b = blue;
 #endif
}
void setPixeldoubleopposites(int Pixel, byte red, byte green, byte blue) {
 #ifndef ADAFRUIT_NEOPIXEL_H
   // FastLED
   leds[0][Pixel].r = red;
   leds[0][Pixel].g = green;
   leds[0][Pixel].b = blue;

   leds[1][NUM_LEDS-Pixel].r = red;
   leds[1][NUM_LEDS-Pixel].g = green;
   leds[1][NUM_LEDS-Pixel].b = blue;
 #endif
}

void setPixeldouble(int Pixel, byte red, byte green, byte blue) {
 #ifndef ADAFRUIT_NEOPIXEL_H
   // FastLED
   leds[0][Pixel].r = red;
   leds[0][Pixel].g = green;
   leds[0][Pixel].b = blue;

   leds[1][Pixel].r = red;
   leds[1][Pixel].g = green;
   leds[1][Pixel].b = blue;
 #endif
}

void setAll(byte red, byte green, byte blue, int strip) {
  for(int i = 0 ; i < NUM_LEDS; i++ ) {
    setPixel(i, red, green, blue, strip);
  }
    showStrip();
}
void setEverything(byte red, byte green, byte blue) {
  for (int s = 0; s < 2; s++) {
    for(int i = 0 ; i < NUM_LEDS; i++ ) {
      setPixel(i, red, green, blue, s);
    }
  }
    showStrip();
}

void showStrip() {
 #ifdef ADAFRUIT_NEOPIXEL_H
   // NeoPixel
   strip.show();
 #endif
 #ifndef ADAFRUIT_NEOPIXEL_H
   // FastLED
   FastLED.show();
 #endif
}

bool isInterrupted() {
  return Serial.available() > 0;
}