package com.example.sensor.sensorapp;

public class fifo {
    private int Qlength ;
    private int[] myQuence;

    public fifo(int Qlength) {
        this.Qlength = Qlength ;
        myQuence = new int [Qlength] ;
    }

    public void addElement(int newElement) {
        for(int i=Qlength-1; i>0; i--){
            myQuence[i] = myQuence[i-1];
        }
        myQuence[0] = newElement;
    }

    public int returnSum() {
        int sum = 0;
        for(int i=0;i<Qlength;i++){
            sum = sum + myQuence[i];
        }
        return sum;
    }

    public int returnElement(int index){
        int Element = 0 ;
        try{
            Element = myQuence[index];
        }catch(ArrayIndexOutOfBoundsException e){
            e.printStackTrace();
        }
        return Element;
    }

    public boolean isFull(){
        if(myQuence[Qlength-1] != 0){
            return true ;
        }
        else{
            return false ;
        }
    }
}
