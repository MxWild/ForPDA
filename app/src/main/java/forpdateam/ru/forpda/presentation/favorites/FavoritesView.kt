package forpdateam.ru.forpda.presentation.favorites

import forpdateam.ru.forpda.api.favorites.models.FavData
import forpdateam.ru.forpda.api.favorites.models.FavItem
import forpdateam.ru.forpda.common.mvp.IBaseView

/**
 * Created by radiationx on 01.01.18.
 */

interface FavoritesView : IBaseView {
    fun onLoadFavorites(data: FavData)
    fun onShowFavorite(items: List<FavItem>)
    fun onHandleEvent(count: Int)
    fun showItemDialogMenu(item: FavItem)
    fun changeFav(action: Int, type: String, favId: Int)
    fun showSubscribeDialog(item: FavItem)
}