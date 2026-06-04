import os
import matplotlib.pyplot as plt
import pandas as pd
import seaborn as sns

# ==========================================
#               KONFIGURACJA
# ==========================================
CSV_FILE_PATH = "raport_SSFR_-_Gauss_Blur_Size_surowe_klatki.csv"  # Nazwa lub ścieżka do Twojego pliku CSV
COMBINE_PLOTS = True  # True: wszystkie na jednym wykresie | False: osobne wykresy jeden pod drugim

# Ustawienie czytelnego stylu wykresów
sns.set_theme(style="whitegrid")


def generuj_wykresy_wydajnosci(sciezka_pliku, polacz_wykresy=True):
    # Bezpieczne wczytanie pliku
    if not os.path.exists(sciezka_pliku):
        print(f"❌ Błąd: Nie znaleziono pliku '{sciezka_pliku}' w bieżącym katalogu.")
        return

    # Wczytanie danych z usunięciem ewentualnych spacji w nagłówkach
    df = pd.read_csv(sciezka_pliku)
    df.columns = df.columns.str.strip()

    # Walidacja poprawności kolumn
    wymagane_kolumny = ["Scenariusz", "Numer Klatki", "Czas Klatki (ms)"]
    if not all(kolumna in df.columns for kolumna in wymagane_kolumny):
        print(f"❌ Błąd: Plik CSV musi zawierać kolumny: {wymagane_kolumny}")
        print(f"Znaleziono kolumny: {list(df.columns)}")
        return

    # Pobranie unikalnych scenariuszy z pliku
    scenariusze = df["Scenariusz"].unique()

    if polacz_wykresy:
        # --- WARIANT 1: Wszystkie serie danych na jednym wykresie zbiorczym ---
        plt.figure(figsize=(14, 6))

        sns.lineplot(
            data=df,
            x="Numer Klatki",
            y="Czas Klatki (ms)",
            hue="Scenariusz",
            linewidth=1.5,
            alpha=0.85,
        )

        plt.title(
            "Przebieg czasów generowania klatek",
            fontsize=14,
            fontweight="bold",
            pad=15,
        )
        plt.xlabel("Numer klatki", fontsize=12, fontweight="bold")
        plt.ylabel("Czas klatki (ms)", fontsize=12, fontweight="bold")

        # Umieszczenie legendy na zewnątrz wykresu, aby nie zasłaniała danych
        plt.legend(
            title="Badany scenariusz", bbox_to_anchor=(1.02, 1), loc="upper left"
        )
        plt.tight_layout()

        # Opcjonalnie: automatyczne zapisywanie do pliku (wygodne do LaTeXa)
        # plt.savefig('wykres_zbiorczy_frametime.png', dpi=300, bbox_inches='tight')
        plt.show()

    else:
        # --- WARIANT 2: Osobne subplots (wykresy) dla każdego scenariusza ---
        liczba_scenariuszy = len(scenariusze)

        # Generowanie siatki wykresów (wspólna oś X dla łatwiejszego porównywania)
        fig, axes = plt.subplots(
            liczba_scenariuszy,
            1,
            figsize=(14, 3.5 * liczba_scenariuszy),
            sharex=True,
        )

        # Zabezpieczenie na wypadek, gdyby w pliku był tylko 1 scenariusz
        if liczba_scenariuszy == 1:
            axes = [axes]

        # Paleta kolorów dla urozmaicenia osobnych wykresów
        paleta_kolorow = sns.color_palette("muted", n_colors=liczba_scenariuszy)

        for i, scenariusz in enumerate(scenariusze):
            df_filtrowany = df[df["Scenariusz"] == scenariusz]
            ax = axes[i]

            sns.lineplot(
                data=df_filtrowany,
                x="Numer Klatki",
                y="Czas Klatki (ms)",
                ax=ax,
                color=paleta_kolorow[i],
                linewidth=1.2,
            )

            # Stylizacja pojedynczego podwykresu
            ax.set_title(
                f"Scenariusz: {scenariusz}",
                fontsize=12,
                fontweight="bold",
                loc="left",
            )
            ax.set_ylabel("Czas (ms)", fontsize=11)
            ax.grid(True, linestyle="--", alpha=0.5)

        # Dodanie opisu osi X tylko dla najniższego wykresu w hierarchii
        axes[-1].set_xlabel("Numer klatki", fontsize=12, fontweight="bold")

        plt.tight_layout()

        # Opcjonalnie: automatyczne zapisywanie do pliku
        # plt.savefig('wykresy_osobne_frametime.png', dpi=300, bbox_inches='tight')
        plt.show()


# Uruchomienie skryptu
if __name__ == "__main__":
    generuj_wykresy_wydajnosci(CSV_FILE_PATH, polacz_wykresy=COMBINE_PLOTS)