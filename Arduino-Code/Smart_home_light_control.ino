#include <SoftwareSerial.h>

SoftwareSerial BT(10,11);

char data;

void setup()
{
  pinMode(8, OUTPUT);
  pinMode(9, OUTPUT);

  digitalWrite(8, LOW);
  digitalWrite(9, LOW);

  BT.begin(9600);
}

void loop()
{
  if(BT.available())
  {
    data = BT.read();

    if(data=='A')
      digitalWrite(8,HIGH);

    if(data=='a')
      digitalWrite(8,LOW);

    if(data=='B')
      digitalWrite(9,HIGH);

    if(data=='b')
      digitalWrite(9,LOW);

    if(data=='C')
    {
      digitalWrite(8,HIGH);
      digitalWrite(9,HIGH);
    }

    if(data=='c')
    {
      digitalWrite(8,LOW);
      digitalWrite(9,LOW);
    }
  }
}
