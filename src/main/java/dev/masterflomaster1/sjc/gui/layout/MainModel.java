package dev.masterflomaster1.sjc.gui.layout;

import dev.masterflomaster1.sjc.gui.event.DefaultEventBus;
import dev.masterflomaster1.sjc.gui.event.NavEvent;
import dev.masterflomaster1.sjc.gui.page.Page;
import dev.masterflomaster1.sjc.gui.page.components.*;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2OutlinedAL;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MainModel {

    public static final Class<? extends Page> DEFAULT_PAGE = AllHashPage.class;

    private static final Map<Class<? extends Page>, NavTree.Item> NAV_TREE = createNavItems();

    NavTree.Item getTreeItemForPage(Class<? extends Page> pageClass) {
        return NAV_TREE.getOrDefault(pageClass, NAV_TREE.get(DEFAULT_PAGE));
    }

    List<NavTree.Item> findPages(String filter) {
        return NAV_TREE.values().stream()
            .filter(item -> item.getValue() != null && item.getValue().matches(filter))
            .toList();
    }

    public MainModel() {
        DefaultEventBus.getInstance().subscribe(NavEvent.class, e -> navigate(e.getPage()));
    }

    private final ReadOnlyObjectWrapper<Class<? extends Page>> selectedPage = new ReadOnlyObjectWrapper<>();

    public ReadOnlyObjectProperty<Class<? extends Page>> selectedPageProperty() {
        return selectedPage.getReadOnlyProperty();
    }

    private final ReadOnlyObjectWrapper<NavTree.Item> navTree = new ReadOnlyObjectWrapper<>(createTree());

    public ReadOnlyObjectProperty<NavTree.Item> navTreeProperty() {
        return navTree.getReadOnlyProperty();
    }

    private NavTree.Item createTree() {
        var classicalGroup = NavTree.Item.group("Classical Cryptography", new FontIcon(Material2OutlinedAL.LOCK));
        classicalGroup.getChildren().setAll(
                NAV_TREE.get(ADFGVXPage.class),
                NAV_TREE.get(AtbashPage.class),
                NAV_TREE.get(AffinePage.class),
                NAV_TREE.get(CaesarPage.class),
                NAV_TREE.get(EnigmaPage.class),
                NAV_TREE.get(PlayfairCipherPage.class),
                NAV_TREE.get(VigenereCipherPage.class)
        );
        classicalGroup.setExpanded(true);

        var symmetricGroup = NavTree.Item.group("Symmetric Encryption", new FontIcon(Material2OutlinedAL.LOCK));
        symmetricGroup.getChildren().setAll(
                NAV_TREE.get(BlockCipherPage.class)
        );

        var hashGroup = NavTree.Item.group("Unkeyed Hash Functions", new FontIcon(Material2OutlinedAL.LOCK));
        hashGroup.getChildren().setAll(
                NAV_TREE.get(AllHashPage.class),
                NAV_TREE.get(HashFilesPage.class)
        );

        var macGroup = NavTree.Item.group("Message Authentication Code", new FontIcon(Material2OutlinedAL.LOCK));
        macGroup.getChildren().setAll(NAV_TREE.get(HmacPage.class));

        var passwordGroup = NavTree.Item.group("Passwords", new FontIcon(Material2OutlinedAL.LOCK));
        passwordGroup.getChildren().setAll(
                NAV_TREE.get(BCryptPage.class),
                NAV_TREE.get(Pbkdf2Page.class),
                NAV_TREE.get(PasswordMeterPage.class)
        );

        var root = NavTree.Item.root();
        root.getChildren().setAll(
            classicalGroup,
            symmetricGroup,
            hashGroup,
            macGroup,
            passwordGroup
        );

        return root;
    }

    public static Map<Class<? extends Page>, NavTree.Item> createNavItems() {
        var map = new HashMap<Class<? extends Page>, NavTree.Item>();

        map.put(ADFGVXPage.class, NavTree.Item.page(ADFGVXPage.NAME, ADFGVXPage.class));
        map.put(PlayfairCipherPage.class, NavTree.Item.page(PlayfairCipherPage.NAME, PlayfairCipherPage.class));
        map.put(AtbashPage.class, NavTree.Item.page(AtbashPage.NAME, AtbashPage.class));
        map.put(EnigmaPage.class, NavTree.Item.page(EnigmaPage.NAME, EnigmaPage.class));
        map.put(AffinePage.class, NavTree.Item.page(AffinePage.NAME, AffinePage.class));
        map.put(CaesarPage.class, NavTree.Item.page(CaesarPage.NAME, CaesarPage.class));
        map.put(VigenereCipherPage.class, NavTree.Item.page(VigenereCipherPage.NAME, VigenereCipherPage.class));
        map.put(BlockCipherPage.class, NavTree.Item.page(BlockCipherPage.NAME, BlockCipherPage.class));
        map.put(AllHashPage.class, NavTree.Item.page(AllHashPage.NAME, AllHashPage.class));
        map.put(HashFilesPage.class, NavTree.Item.page(HashFilesPage.NAME, HashFilesPage.class));
        map.put(HmacPage.class, NavTree.Item.page(HmacPage.NAME, HmacPage.class));
        map.put(BCryptPage.class, NavTree.Item.page(BCryptPage.NAME, BCryptPage.class));
        map.put(PasswordMeterPage.class, NavTree.Item.page(PasswordMeterPage.NAME, PasswordMeterPage.class));
        map.put(Pbkdf2Page.class, NavTree.Item.page(Pbkdf2Page.NAME, Pbkdf2Page.class));

        return map;
    }

    public void navigate(Class<? extends Page> page) {
        selectedPage.set(Objects.requireNonNull(page));
    }

}
