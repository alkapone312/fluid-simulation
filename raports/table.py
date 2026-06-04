import pandas as pd
import os

# ==========================================
# KONFIGURACJA
# ==========================================

# Parametry próby kontrolnej (bazy)
CONTROL_TIME = 12.1
CONTROL_FPS = 1000.0 / CONTROL_TIME  # ~82.64 FPS
CONTROL_STD_DEV = 0.3472
ONE_PERCENT_LOW = 12.7356
ONE_TENTH_PERCENT_LOW = 13.5084

# Lista plików ogólnych do przetworzenia.
FILES_OGOLNE = [
    # 'raport_Control_set_ogolny.csv',
    # 'raport_Particle_Size_curvature_ogolny.csv',
    # 'raport_Particle_Size_gauss_ogolny.csv',
    # 'raport_Particle_Size_ogolny.csv',
    'raport_Perspective_Volume_-_Fluid_Density_ogolny.csv',
    'raport_Perspective_Volume_-_Particle_Radius_ogolny.csv',
    'raport_Perspective_Volume_-_Resolution_ogolny.csv',
    # 'raport_SSFR_-_Curvature_Flow_-_Number_of_iterations_ogolny.csv',
    # 'raport_SSFR_Curvature_Flow_-_Step_size_ogolny.csv',
    # 'raport_SSFR_-_Gauss_Blur_Size_ogolny.csv',
    # 'raport_SSFR_-_Gauss_Blur_Smoothness_ogolny.csv',
]

# ==========================================
# GŁÓWNA LOGIKA
# ==========================================

def generate_multi_column_gpu_latex(files_list):
    all_data_frames = []
    wyszukane_etapy = set()

    for file_ogolny in files_list:
        file_gpu = file_ogolny.replace('_ogolny.csv', '_gpu.csv')

        if not os.path.exists(file_ogolny) or not os.path.exists(file_gpu):
            print(f"% [Ostrzeżenie] Pominięto parę plików dla: {file_ogolny}")
            continue

        # Wczytanie danych
        df_ogolny = pd.read_csv(file_ogolny)
        df_gpu = pd.read_csv(file_gpu)

        # Wykrywanie nazwy kolumny czasu GPU (obsługa nowej i starej wersji skryptu pomiarowego)
        target_gpu_col = 'Sredni Czas (ms)'
        if target_gpu_col not in df_gpu.columns:
            if 'Sredni Czas na GPU (ms)' in df_gpu.columns:
                target_gpu_col = 'Sredni Czas na GPU (ms)'
            else:
                print(f"% [Błąd] Nie znaleziono kolumny czasu w pliku: {file_gpu}")
                continue

        # 1. Transformacja tabeli GPU: zamiana wierszy etapów na osobne kolumny (Pivot)
        df_gpu_pivoted = df_gpu.pivot(
            index='Scenariusz',
            columns='Nazwa Shadera (Pass)',
            values=target_gpu_col
        ).reset_index()

        # Zapamiętanie nazw shaderów w celu stworzenia nagłówków
        etapy_w_pliku = [col for col in df_gpu_pivoted.columns if col != 'Scenariusz']
        wyszukane_etapy.update(etapy_w_pliku)

        # 2. Połączenie danych ogólnych z nowymi kolumnami etapów GPU
        df_merged = pd.merge(df_ogolny, df_gpu_pivoted, on='Scenariusz', how='left')
        all_data_frames.append(df_merged)

    if not all_data_frames:
        print("% Błąd: Brak prawidłowych danych.")
        return

    # Scalenie wszystkich tabel w jeden DataFrame
    final_df = pd.concat(all_data_frames, ignore_index=True)

    # Sortowanie nazw etapów, aby kolumny były w stałej kolejności
    list_etapow = sorted(list(wyszukane_etapy))

    # Sprawdzenie obecności nowych kolumn statystycznych w pliku ogólnym
    has_stats = "Odchylenie Std (ms)" in final_df.columns

    # ==========================================
    # GENEROWANIE KODU LATEX
    # ==========================================

    # Określenie struktury kolumn (l dla scenariusza, c dla wartości)
    # Jeśli są statystyki, dodajemy kolumny: StdDev, 1% Low, 0.1% Low (pomijamy surowe min/max dla czystości tabeli)
    podstawowe_kolumny = 5 if has_stats else 2
    kolumny_format = "l" + "c" * (podstawowe_kolumny + len(list_etapow))

    lines = []
    lines.append(r"\begin{table}[htbp]")
    lines.append(r"  \centering")
    lines.append(r"  \caption{Szczegółowe zestawienie wydajności i stabilności z rozbiciem na etapy GPU}")
    lines.append(r"  \label{tab:szczegoly_gpu}")
    lines.append(f"  \\resizebox{{\\textwidth}}{{!}}{{")
    lines.append(f"  \\begin{{tabular}}{{{kolumny_format}}}")
    lines.append(r"    \hline")

    # Budowanie nagłówka tabeli
    naglowek = r"    \textbf{Scenariusz} & \textbf{Czas Klatki [ms]} & \textbf{Średnie FPS}"
    if has_stats:
        naglowek += r" & \textbf{Odchylenie Std [ms]} & \textbf{1\% Low [ms]} & \textbf{0.1\% Low [ms]}"

    for etap in list_etapow:
        # Bezpieczne parsowanie nazw shaderów/passów do standardu LaTeX
        etap_safe = etap.replace('_', r'\_').replace('%', r'\%')
        naglowek += f" & \\textbf{{{etap_safe} [ms]}}"
    naglowek += r" \\"

    lines.append(naglowek)
    lines.append(r"    \hline")

    # Wiersz próby kontrolnej
    puste_etapy_kontroli = " & —" * len(list_etapow)
    if has_stats:
        # W próbie kontrolnej nie liczyliśmy zaawansowanych statystyk klatki z listy, więc wstawiamy kreski
        lines.append(f"    Próba kontrolna & {CONTROL_TIME:.2f} & {CONTROL_FPS:.2f} & {CONTROL_STD_DEV:.2f} & {ONE_PERCENT_LOW:.2f} & {ONE_TENTH_PERCENT_LOW:.2f}{puste_etapy_kontroli} \\\\")
    else:
        lines.append(f"    Próba kontrolna & {CONTROL_TIME:.2f} & {CONTROL_FPS:.2f}{puste_etapy_kontroli} \\\\")
    lines.append(r"    \hline")

    # Wiersze z danymi
    for _, row in final_df.iterrows():
        scenariusz = str(row['Scenariusz']).replace('_', r'\_').replace('%', r'\%')
        czas_klatki = row['Sredni Czas Klatki (ms)']
        fps = row['Srednie FPS']

        linia_danych = f"    {scenariusz} & {czas_klatki:.2f} & {fps:.2f}"

        # Jeśli plik CSV zawiera zaawansowane metryki, doklejamy je do rzędu
        if has_stats:
            std_dev = row['Odchylenie Std (ms)']
            low_1 = row['1% Low (ms)']
            low_01 = row['0.1% Low (ms)']
            linia_danych += f" & {std_dev:.2f} & {low_1:.2f} & {low_01:.2f}"

        # Dynamiczne dodawanie wartości dla każdej kolumny shadera
        for etap in list_etapow:
            wartosc = row.get(etap)
            if pd.isna(wartosc):
                linia_danych += " & —"
            else:
                linia_danych += f" & {wartosc:.2f}"

        linia_danych += r" \\"
        lines.append(linia_danych)

    lines.append(r"    \hline")
    lines.append(r"  \end{tabular}")
    lines.append(r"  }")
    lines.append(r"\end{table}")

    # Wypisanie kodu tabeli
    print("\n".join(lines))

if __name__ == "__main__":
    generate_multi_column_gpu_latex(FILES_OGOLNE)