package forpdateam.ru.forpda.ui.fragments.mentions;


import forpdateam.ru.forpda.api.mentions.models.MentionsData;
import forpdateam.ru.forpda.common.mvp.IBasePresenter;
import forpdateam.ru.forpda.common.mvp.IBaseView;

/**
 * Created by radiationx on 11.11.17.
 */

public interface MentionsContract {
    interface View extends IBaseView {
        void showMentions(MentionsData data);
    }

    interface Presenter extends IBasePresenter<View> {
        void getMentions(int st);
    }
}
