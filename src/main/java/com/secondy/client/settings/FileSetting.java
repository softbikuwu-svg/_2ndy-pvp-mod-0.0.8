package com.secondy.client.settings;

import com.google.gson.JsonObject;

import java.util.List;
import java.util.function.Supplier;

/**
 * Выбор файла из папки. Список читается с диска только по явному обновлению (клик, загрузка конфига,
 * кнопка «Обновить»), а не каждый кадр. В конфиг пишется имя файла, а не номер.
 */
public class FileSetting extends Setting {
    private final Supplier<List<String>> lister;
    private List<String> options = List.of();
    private String selected = "";
    private Runnable onChange = () -> {};

    public FileSetting(String name, String key, Supplier<List<String>> lister) {
        super(name, key);
        this.lister = lister;
        refresh();
    }

    /** Вызывается после того, как игрок переключил файл кликом (например, чтобы проиграть превью). */
    public FileSetting onChange(Runnable r) {
        this.onChange = r;
        return this;
    }

    public void refresh() {
        try {
            options = lister.get();
        } catch (Exception e) {
            options = List.of();
        }
    }

    /** Файл, который реально будет использоваться, или null, если подходящего нет. */
    public String current() {
        if (options.contains(selected)) return selected;
        if (selected.isEmpty() && !options.isEmpty()) return options.get(0);
        return null;
    }

    /** Текст для кнопки в меню. */
    public String display() {
        String cur = current();
        if (cur != null) return cur;
        if (!selected.isEmpty()) return selected + " (file missing)";
        return "no files";
    }

    public void next() {
        refresh();
        if (options.isEmpty()) return;
        String cur = current();
        int i = cur == null ? -1 : options.indexOf(cur);
        selected = options.get((i + 1) % options.size());
        onChange.run();
    }

    @Override public void save(JsonObject o) { o.addProperty(key, selected); }

    @Override public void load(JsonObject o) {
        if (o.has(key)) selected = o.get(key).getAsString();
        refresh();
    }
}
