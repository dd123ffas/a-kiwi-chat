package kim.biryeong.textcenterer.chat;

public interface ChatHeadAccess {
    HeadDetection.Result textCenterer$head();
    void textCenterer$head(HeadDetection.Result head);
}
