import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
import os

# Ustawienia stylu wykresów dla lepszej czytelności
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
        print(f"⚠️ Brak plików dla: '{title}'. Upewnij się, że nazwy się zgadzają i pliki są w tym samym folderze.")
        continue

    # Wczytanie danych z plików CSV
    df_ogolny = pd.read_csv(file_ogolny)
    df_gpu = pd.read_csv(file_gpu)

    # Utworzenie układu dwóch wykresów obok siebie w jednym oknie
    fig, axes = plt.subplots(1, 2, figsize=(16, 7))
    fig.suptitle(title, fontsize=18, fontweight='bold', y=1.02)

    # --- WYKRES 1: FPS vs Czas Klatki (Ogolny) ---
    ax1 = axes[0]
    ax2 = ax1.twinx()  # Utworzenie drugiej osi Y dla czasu klatki

    # Rysowanie słupków dla FPS na głównej osi (niebieski)
    sns.barplot(data=df_ogolny, x="Scenariusz", y="Srednie FPS", ax=ax1, color="#4C72B0", alpha=0.85)

    # Rysowanie linii dla czasu klatki na prawej osi (czerwony)
    sns.lineplot(data=df_ogolny, x="Scenariusz", y="Sredni Czas Klatki (ms)", ax=ax2, color="#C44E52", marker="o", markersize=8, linewidth=2.5)

    # Formatowanie osi Wykresu 1
    ax1.set_title("Wydajność ogólna: FPS i Czas Klatki", fontsize=14, pad=10)
    ax1.set_ylabel("Średnie FPS", color="#4C72B0", fontweight='bold', fontsize=12)
    ax2.set_ylabel("Średni Czas Klatki (ms)", color="#C44E52", fontweight='bold', fontsize=12)
    ax1.set_xlabel("")
    plt.setp(ax1.get_xticklabels(), rotation=45, ha="right", rotation_mode="anchor")
    ax1.grid(axis='y', linestyle='--', alpha=0.7)
    ax2.grid(False) # Wyłączamy siatkę na drugiej osi, by się nie nakładały

    # --- WYKRES 2: Czas GPU z podziałem na Passy (Skumulowany) ---
    ax3 = axes[1]

    # Przekształcenie danych (pivot) do stworzenia wykresu skumulowanego (stacked bar chart)
    gpu_pivot = df_gpu.pivot(index="Scenariusz", columns="Nazwa Shadera (Pass)", values="Sredni Czas na GPU (ms)")

    # Uporządkowanie wierszy z zachowaniem kolejności rosnącej/testowej z pliku ogólnego
    gpu_pivot = gpu_pivot.reindex(df_ogolny["Scenariusz"])

    # Generowanie wykresu skumulowanego z paletą 'viridis' ułatwiającą rozróżnienie passów
    gpu_pivot.plot(kind="bar", stacked=True, ax=ax3, colormap="viridis", edgecolor="none")

    # Formatowanie osi Wykresu 2
    ax3.set_title("Czas wykonania na GPU wg. wywołań shaderów", fontsize=14, pad=10)
    ax3.set_ylabel("Czas na GPU (ms)", fontsize=12)
    ax3.set_xlabel("")
    plt.setp(ax3.get_xticklabels(), rotation=45, ha="right", rotation_mode="anchor")

    # Przeniesienie legendy poza obszar wykresu, żeby nie zasłaniała danych
    ax3.legend(title="Wywołanie Shadera", bbox_to_anchor=(1.02, 1), loc='upper left')
    ax3.grid(axis='y', linestyle='--', alpha=0.7)

    # Dopasowanie marginesów i wyświetlenie wykresów
    plt.tight_layout()
    plt.show()