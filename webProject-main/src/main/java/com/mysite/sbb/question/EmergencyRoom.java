package com.mysite.sbb.question;

public class EmergencyRoom {

    public String dutyName; //기관이름

    public String dutyTel3; //전화번호

    public String hvamyn;  //구급차가용여부

    public String hvec; //응급실 갯수

    public EmergencyRoom(String dutyName, String dutyTel3, String hvamyn, String hvec) {
        this.dutyName = dutyName;
        this.dutyTel3 = dutyTel3;
        this.hvamyn = hvamyn;
        this.hvec = hvec;
    }
}
