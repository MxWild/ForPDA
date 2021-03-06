package forpdateam.ru.forpda.ui.fragments.notes;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import forpdateam.ru.forpda.App;
import forpdateam.ru.forpda.R;
import forpdateam.ru.forpda.api.RequestFile;
import forpdateam.ru.forpda.common.FilePickHelper;
import forpdateam.ru.forpda.common.IntentHandler;
import forpdateam.ru.forpda.common.Utils;
import forpdateam.ru.forpda.data.models.notes.NoteItem;
import forpdateam.ru.forpda.data.realm.notes.NoteItemBd;
import forpdateam.ru.forpda.ui.TabManager;
import forpdateam.ru.forpda.ui.fragments.RecyclerFragment;
import forpdateam.ru.forpda.ui.fragments.devdb.BrandFragment;
import forpdateam.ru.forpda.ui.fragments.notes.adapters.NotesAdapter;
import forpdateam.ru.forpda.ui.views.ContentController;
import forpdateam.ru.forpda.ui.views.DynamicDialogMenu;
import forpdateam.ru.forpda.ui.views.FunnyContent;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Created by radiationx on 06.09.17.
 */

public class NotesFragment extends RecyclerFragment implements NotesAdapter.OnItemClickListener<NoteItem>, NotesAddPopup.NoteActionListener {
    private NotesAdapter adapter;
    private Realm realm;
    private DynamicDialogMenu<NotesFragment, NoteItem> dialogMenu;

    public NotesFragment() {
        configuration.setDefaultTitle(App.get().getString(R.string.fragment_title_notes));
        configuration.setUseCache(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        realm = Realm.getDefaultInstance();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setCardsBackground();
        adapter = new NotesAdapter();
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        refreshLayout.setOnRefreshListener(this::loadCacheData);
        recyclerView.addItemDecoration(new BrandFragment.SpacingItemDecoration(App.px8, false));
        viewsReady();
    }

    @Override
    protected void addBaseToolbarMenu(Menu menu) {
        super.addBaseToolbarMenu(menu);
        menu
                .add(R.string.add)
                .setIcon(App.getVecDrawable(getContext(), R.drawable.ic_toolbar_add))
                .setOnMenuItemClickListener(item -> {
                    new NotesAddPopup(getContext(), null, this);
                    return true;
                })
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu
                .add(R.string.import_s)
                .setOnMenuItemClickListener(item -> {
                    App.get().checkStoragePermission(() -> {
                        startActivityForResult(FilePickHelper.pickFile(false), REQUEST_PICK_FILE);
                    }, App.getActivity());
                    return true;
                });
        menu
                .add(R.string.export_s)
                .setOnMenuItemClickListener(item -> {
                    App.get().checkStoragePermission(this::exportNotes, App.getActivity());
                    return true;
                });

    }

    @Override
    public void loadCacheData() {
        super.loadCacheData();
        if (!realm.isClosed()) {
            setRefreshing(true);
            RealmResults<NoteItemBd> results = realm.where(NoteItemBd.class).findAllSorted("id", Sort.DESCENDING);

            if (results.isEmpty()) {
                if (!contentController.contains(ContentController.TAG_NO_DATA)) {
                    FunnyContent funnyContent = new FunnyContent(getContext())
                            .setImage(R.drawable.ic_bookmark)
                            .setTitle(R.string.funny_notes_nodata_title);
                    contentController.addContent(funnyContent, ContentController.TAG_NO_DATA);
                }
                contentController.showContent(ContentController.TAG_NO_DATA);
            } else {
                contentController.hideContent(ContentController.TAG_NO_DATA);
            }

            ArrayList<NoteItem> nonBdResult = new ArrayList<>();
            for (NoteItemBd item : results) {
                nonBdResult.add(new NoteItem(item));
            }
            adapter.addAll(nonBdResult);
        }
        setRefreshing(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        realm.close();
    }

    @Override
    public void onAddNote(NoteItem item) {
        if (realm.isClosed())
            return;
        realm.executeTransactionAsync(realm1 -> {
            NoteItemBd itemBd = realm1.where(NoteItemBd.class).equalTo("id", item.getId()).findFirst();
            if (itemBd != null) {
                itemBd.setTitle(item.getTitle());
                itemBd.setLink(item.getLink());
                itemBd.setContent(item.getContent());
            } else {
                itemBd = new NoteItemBd(item);
            }
            realm1.insertOrUpdate(itemBd);
        }, this::loadCacheData);
    }

    public void deleteNote(long id) {
        if (realm.isClosed())
            return;
        realm.executeTransactionAsync(realm1 -> {
            realm1.where(NoteItemBd.class)
                    .equalTo("id", id)
                    .findAll()
                    .deleteAllFromRealm();
        }, this::loadCacheData);
    }


    public static void addNote(NoteItem item) {
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransactionAsync(realm1 -> {
            realm1.insertOrUpdate(new NoteItemBd(item));
        }, () -> {
            realm.close();
            NotesFragment notesFragment = (NotesFragment) TabManager.get().getByClass(NotesFragment.class);
            if (notesFragment == null) {
                return;
            }
            notesFragment.loadCacheData();
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (data == null) {
                //Display an error
                return;
            }
            if (requestCode == REQUEST_PICK_FILE) {
                List<RequestFile> files = FilePickHelper.onActivityResult(getContext(), data);
                RequestFile file = files.get(0);
                if (file.getFileName().matches("[\\s\\S]*?\\.json$")) {
                    BufferedReader r = new BufferedReader(new InputStreamReader(file.getFileStream()));
                    StringBuilder total = new StringBuilder();
                    String line;
                    try {
                        while ((line = r.readLine()) != null) {
                            total.append(line).append('\n');
                        }
                        r.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Ошибка при чтении файла", Toast.LENGTH_SHORT).show();
                    }
                    importNotes(total.toString());
                } else {
                    Toast.makeText(getContext(), "Файл имеет неправильное расширение", Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == REQUEST_SAVE_FILE) {

            }
        }
    }

    private void importNotes(String jsonSource) {
        ArrayList<NoteItem> noteItems = new ArrayList<>();
        try {
            final JSONArray jsonBody = new JSONArray(jsonSource);
            for (int i = 0; i < jsonBody.length(); i++) {
                try {
                    JSONObject jsonItem = jsonBody.getJSONObject(i);
                    NoteItem item = new NoteItem();
                    item.setId(jsonItem.getLong("id"));
                    item.setTitle(jsonItem.getString("title"));
                    item.setLink(jsonItem.getString("link"));
                    item.setContent(jsonItem.getString("content"));
                    noteItems.add(item);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Ошибка разбора файла: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        Toast.makeText(getContext(), "Заметки успешно импортированы", Toast.LENGTH_SHORT).show();
        if (realm.isClosed()) return;
        realm.executeTransactionAsync(realm1 -> {
            for (NoteItem item : noteItems) {
                realm1.insertOrUpdate(new NoteItemBd(item));
            }
        }, this::loadCacheData);
    }

    private void exportNotes() {
        if (realm.isClosed())
            return;
        RealmResults<NoteItemBd> results = realm.where(NoteItemBd.class).findAllSorted("id", Sort.DESCENDING);

        ArrayList<NoteItem> noteItems = new ArrayList<>();
        for (NoteItemBd item : results) {
            noteItems.add(new NoteItem(item));
        }

        final JSONArray jsonBody = new JSONArray();
        for (NoteItem item : noteItems) {
            try {
                JSONObject jsonItem = new JSONObject();
                jsonItem.put("id", item.getId());
                jsonItem.put("title", item.getTitle());
                jsonItem.put("link", item.getLink());
                jsonItem.put("content", item.getContent());
                jsonBody.put(jsonItem);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        saveImageToExternalStorage(jsonBody.toString());
    }

    private void saveImageToExternalStorage(String json) {
        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).toString();
        String date = new SimpleDateFormat("MMddyyy-HHmmss", Locale.getDefault()).format(new Date(System.currentTimeMillis()));
        String fileName = "ForPDA_Notes_" + date + ".json";
        File file = new File(root, fileName);
        if (file.exists())
            file.delete();
        try {
            FileOutputStream fOut = new FileOutputStream(file);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append(json);

            myOutWriter.close();

            fOut.flush();
            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Файл не сохранён: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        Toast.makeText(getContext(), "Заметки успешно экспортированы в " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onItemClick(NoteItem item) {
        try {
            IntentHandler.handle(item.getLink());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public boolean onItemLongClick(NoteItem item) {
        if (dialogMenu == null) {
            dialogMenu = new DynamicDialogMenu<>();
            dialogMenu.addItem(getString(R.string.copy_link), (context, data) -> {
                Utils.copyToClipBoard("" + data.getLink());
            });
            dialogMenu.addItem(getString(R.string.edit), (context, data) -> {
                new NotesAddPopup(context.getContext(), data, context);
            });
            dialogMenu.addItem(getString(R.string.delete), (context, data) -> {
                context.deleteNote(data.getId());
            });
        }
        dialogMenu.disallowAll();
        dialogMenu.allowAll();
        dialogMenu.show(getContext(), NotesFragment.this, item);
        return true;
    }
}
