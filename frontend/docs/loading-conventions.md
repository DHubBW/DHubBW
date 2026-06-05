# Loading Conventions (Frontend)

Stand: 2026-06-03

Drei Kategorien — pro UI-Aktion eine wählen, konsistent in der gesamten App.

## 1. Skeleton — für Listen / initiale Daten-Renders

Verwende Platzhalter-Karten (`bg-muted` mit gleicher Höhe wie die echte
Karte), wenn:

- der Bildschirm zum ersten Mal Daten lädt,
- Listen oder Grids gefüllt werden,
- der Layout-Sprung sichtbar wäre, sobald die Daten kommen.

Skeleton **niemals** für Re-Fetch oder Mutationen — dort verschwindet
Inhalt, der schon da war, sonst flackert die UI.

```tsx
{isLoading ? (
  <View className="gap-3">
    {Array.from({ length: 4 }).map((_, i) => (
      <View key={i} className="h-24 rounded-lg bg-muted" />
    ))}
  </View>
) : (
  <List items={items} />
)}
```

## 2. Spinner — für Mutationen / Single-Aktionen

`<ActivityIndicator>` (RN) oder Loading-State im Button-Text, wenn:

- der User auf einen Button drückt und auf Server-Antwort wartet,
- ein Modal eine Aktion bestätigt (Submit, Delete, Save),
- der Bereich mit dem Spinner kleiner als ein Screen ist.

Spinner muss **inline** zur Aktion sein (im Button selbst, neben dem
Toggle), nicht ein blockierendes Vollbild-Overlay.

```tsx
<Button onPress={save} disabled={isPending}>
  {isPending ? <ActivityIndicator size="small" /> : <Text>Speichern</Text>}
</Button>
```

## 3. Optimistic — für Toggles / quick-feedback Aktionen

UI sofort umschalten, Server-Call im Hintergrund. Bei Fehler: zurückrollen
und Toast (`sonner-native`) mit Fehlermeldung.

Geeignet für:

- Like / Bookmark / Favorit-Toggle,
- Status-Switches (z.B. "Anwesend" / "Abwesend"),
- Drag-Sortierung,
- Theme-Switch.

```tsx
const handleToggle = async () => {
  const previous = isFavorited;
  setIsFavorited(!previous);
  try {
    await api.toggleFavorite(id);
  } catch (err) {
    setIsFavorited(previous);
    toast.error('Konnte nicht gespeichert werden');
  }
};
```

## Empty / Error States

Beide werden separat von Loading behandelt:

- **Empty:** Karte mit Illustrations-Icon (lucide), Text und CTA. Beispiel
  Mensa-Screen: "Keine Speisepläne für diese Woche · Andere Woche wählen".
- **Error:** rotes Banner (`bg-destructive/10 text-destructive`) mit
  Retry-Button. Wenn 401/403: redirect auf `/(auth)/login`.

Diese Zustände sind in FE-S3-minimal als Cross-Cutting-Pattern zu testen.
