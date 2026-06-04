import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
import os

# Ustawienia stylu wykresów dla lepszej czytelności akademickiej
sns.set_theme(style="whitegrid")

# Definicja par plików: (plik_ogolny, plik_gpu, tytul_wykresu)
experiments = [
    ("raport_Control_set_ogolny.csv", "raport_Control_set_gpu.csv", "Test kontrolny"),
    ("raport_Particle_Size_gauss_ogolny.csv", "raport_Particle_Size_gauss_gpu.csv", "Wpływ rozmiaru cząsteczek (Particle Size) w wygładzaniu Gaussa"),
    ("raport_Particle_Size_curvature_ogolny.csv", "raport_Particle_Size_curvature_gpu.csv", "Wpływ rozmiaru cząsteczek (Particle Size) w wygładzaniu Curvature Flow"),
    ("raport_SSFR_-_Curvature_Flow_-_Number_of_iterations_ogolny.csv", "raport_SSFR_-_Curvature_Flow_-_Number_of_iterations_gpu.csv", "Curvature Flow - Liczba iteracji"),
    ("raport_SSFR_-_Gauss_Blur_Size_ogolny.csv", "raport_SSFR_-_Gauss_Blur_Size_gpu.csv", "Gaussian Blur - Rozmiar (Size)"),
    ("raport_SSFR_-_Gauss_Blur_Smoothness_ogolny.csv", "raport_SSFR_-_Gauss_Blur_Smoothness_gpu.csv", "Gaussian Blur - Wygładzanie (Smoothness)"),
    ("raport_SSFR_Curvature_Flow_-_Step_size_ogolny.csv", "raport_SSFR_Curvature_Flow_-_Step_size_gpu.csv", "Curvature Flow - Rozmiar kroku (Step Size)"),
    ("raport_Perspective_Volume_-_Resolution_ogolny.csv", "raport_Perspective_Volume_-_Resolution_gpu.csv", "Perspective Volume - Resolution"),
    ("raport_Perspective_Volume_-_Fluid_Density_ogolny.csv", "raport_Perspective_Volume_-_Fluid_Density_gpu.csv", "Perspective Volume - Fluid Density"),
    ("raport_Perspective_Volume_-_Particle_Radius_ogolny.csv", "raport_Perspective_Volume_-_Particle_Radius_gpu.csv", "Perspective Volume - Particle Radius")
]

for file_ogolny, file_gpu, title in experiments:
    if not os.path.exists(file_ogolny) or not os.path.exists(file_gpu):
        print(f"⚠️ Brak plików dla: '{title}'. Upewnij się, że nazwy się zgadzają.")
        continue

    # Wczytanie danych z plików CSV
    df_ogolny = pd.read_csv(file_ogolny)
    df_gpu = pd.read_csv(file_gpu)

    # Rozszerzamy układ do 3 wykresów obok siebie (szerokość 22 cale dla czytelności)
    fig, axes = plt.subplots(1, 3, figsize=(22, 7))
    fig.subplots_adjust(wspace=5.0)
    # --- WYKRES 1: FPS vs Czas Klatki (Ogólny) ---
    ax1 = axes[0]
    ax2 = ax1.twinx()

    sns.barplot(data=df_ogolny, x="Scenariusz", y="Srednie FPS", ax=ax1, color="#4C72B0", alpha=0.85)
    sns.lineplot(data=df_ogolny, x="Scenariusz", y="Sredni Czas Klatki (ms)", ax=ax2, color="#C44E52", marker="o", markersize=6, linewidth=2)

    ax1.set_title("Wydajność ogólna: FPS i Czas Klatki", fontsize=13, pad=10, fontweight='bold')
    ax1.set_ylabel("Średnie FPS", color="#4C72B0", fontweight='bold', fontsize=11)
    ax2.set_ylabel("Średni Czas Klatki (ms)", color="#C44E52", fontweight='bold', fontsize=11)
    ax1.set_xlabel("")
    ax1.tick_params(axis='x', rotation=45)
    ax1.grid(axis='y', linestyle='--', alpha=0.5)
    ax2.grid(False)

    # --- WYKRES 2: Profil Stabilności (Frame Pacing: Średnia vs 1% i 0.1% Low) ---
    ax3 = axes[1]
    scenarios = df_ogolny["Scenariusz"].astype(str)

    # Rysowanie linii dla średniej i wskaźników Low
    sns.lineplot(x=scenarios, y=df_ogolny["Sredni Czas Klatki (ms)"], ax=ax3, color="#555555", marker="s", label="Średni Czas", linewidth=2)
    sns.lineplot(x=scenarios, y=df_ogolny["1% Low (ms)"], ax=ax3, color="#E67E22", marker="^", label="1% Low", linewidth=1.5, linestyle="--")
    sns.lineplot(x=scenarios, y=df_ogolny["0.1% Low (ms)"], ax=ax3, color="#D35400", marker="v", label="0.1% Low", linewidth=1.5, linestyle=":")

    # Zacienienie obszaru fluktuacji klatek (między Min a Max) jako pas wariancji
    if "Min (ms)" in df_ogolny.columns and "Max (ms)" in df_ogolny.columns:
        ax3.fill_between(scenarios, df_ogolny["Min (ms)"], df_ogolny["Max (ms)"], color="#95A5A6", alpha=0.2, label="Zakres skrajny (Min-Max)")

    ax3.set_title("Profil stabilności potoku", fontsize=13, pad=10, fontweight='bold')
    ax3.set_ylabel("Czas renderowania klatki (ms)", fontsize=11)
    ax3.set_xlabel("")
    ax3.tick_params(axis='x', rotation=45)
    ax3.grid(axis='y', linestyle='--', alpha=0.5)
    ax3.legend(loc="upper left", fontsize=10)

    # --- WYKRES 3: Czas GPU z podziałem na Passy (Skumulowany) ---
    ax4 = axes[2]

    # Bezpieczne dopasowanie kolumny (w nowym BenchmarkState kolumna nazywa się "Sredni Czas (ms)")
    v_col = "Sredni Czas (ms)" if "Sredni Czas (ms)" in df_gpu.columns else "Sredni Czas na GPU (ms)"

    # Przekształcenie danych (pivot) do wykresu skumulowanego
    gpu_pivot = df_gpu.pivot(index="Scenariusz", columns="Nazwa Shadera (Pass)", values=v_col)
    gpu_pivot = gpu_pivot.reindex(df_ogolny["Scenariusz"])

    # Rysowanie wykresu
    gpu_pivot.plot(kind="bar", stacked=True, ax=ax4, colormap="viridis", edgecolor="none", alpha=0.9)

    ax4.set_title("Czas wykonania na GPU wg wywołań shaderów", fontsize=13, pad=10, fontweight='bold')
    ax4.set_ylabel("Czas na GPU (ms)", fontsize=11)
    ax4.set_xlabel("")
    ax4.tick_params(axis='x', rotation=45)
    ax4.grid(axis='y', linestyle='--', alpha=0.5)
    ax4.legend(title="Wywołanie Shadera", bbox_to_anchor=(1.02, 1), loc='upper left', fontsize=10)

    # Korekcja ułożenia etykiet osi X dla wszystkich trzech osi
    for ax in [ax1, ax3, ax4]:
        plt.setp(ax.get_xticklabels(), rotation=35, ha="right", rotation_mode="anchor")

    # Automatyczne dopasowanie marginesów i zapętlenie wyświetlania
    plt.tight_layout()

    # Opcjonalnie: automatyczne zapisywanie wykresów do plików graficznych (wygodne do LaTeXa)
    # safe_title = title.replace(" ", "_").replace("(", "").replace(")", "")
    # plt.savefig(f"wykres_{safe_title}.png", dpi=300, bbox_inches='tight')

    plt.show()