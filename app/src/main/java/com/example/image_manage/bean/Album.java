package com.example.image_manage.bean;

public class Album {
    private int id;
    private String albumName;
    private int firstP;
    private int pnum;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    public int getFirstP() {
        return firstP;
    }

    public void setFirstP(int firstP) {
        this.firstP = firstP;
    }

    public int getPnum() {
        return pnum;
    }

    public void setPnum(int pnum) {
        this.pnum = pnum;
    }
}
