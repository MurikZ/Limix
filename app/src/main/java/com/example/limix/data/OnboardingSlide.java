package com.example.limix.data;

public class OnboardingSlide {
    private String emoji;
    private String title;
    private String description;

    public OnboardingSlide(String emoji,String title,String description){
        this.description = description;
        this.emoji = emoji;
        this.title = title;
    }

    public String getEmoji(){
        return emoji;
    }
    public String getTitle(){
        return title;
    }
    public String getDescription(){
        return description;
    }
}
