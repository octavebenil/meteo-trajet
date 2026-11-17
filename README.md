# Météo Trajet

Application Android (Java) qui aide à choisir chaque matin entre **le bus** et **la voiture** selon le risque de pluie sur les trajets.

## Fonctionnalités

- **Localisation automatique** via le `LocationManager` d'Android (pas besoin des services Google Play). Le nom de la ville est affiché quand il peut être trouvé.
- **Météo heure par heure** sur 24 h : température, probabilité de pluie, cumul en mm. Les heures pluvieuses sont surlignées.
- **Mises à jour régulières** :
  - toutes les 15 minutes quand l'application est ouverte ;
  - toutes les heures en arrière-plan (WorkManager) ;
  - glisser vers le bas pour actualiser à la main.
- **Conseil bus / voiture** pour le trajet aller et le trajet retour (plages horaires réglables). Le soir, après le trajet retour, l'écran montre le conseil du lendemain.
- **Notification du matin** à l'heure choisie, avec le conseil du jour.
- **Sonnerie d'alarme** facultative : sonnerie au choix, toujours ou seulement s'il risque de pleuvoir. Elle s'arrête depuis la notification ou au bout d'une minute. Elle ne sonne que si les notifications sont autorisées (sinon il n'y aurait aucun moyen de l'arrêter).
- **Alertes pluie dans la journée** quand la pluie est prévue dans les 3 prochaines heures (une seule alerte par heure pluvieuse).
- **Seuil d'alerte réglable** (30 % par défaut, de 10 à 90 %).
- Fonctionne hors ligne avec les dernières données connues.
- Interface en français, traduction anglaise incluse. Thème clair et sombre.

## Règle de décision

Une heure est considérée comme pluvieuse si :

- la probabilité de précipitation atteint le seuil réglé, **ou**
- le modèle prévoit déjà de la pluie, de la bruine, de la neige ou un orage avec au moins 0,1 mm.

Si au moins une heure des plages aller ou retour est pluvieuse, l'application conseille **la voiture**. Sinon, **le bus**.

## Source des données

Les prévisions viennent de l'API gratuite [Open-Meteo](https://open-meteo.com/). Aucune clé API n'est nécessaire et aucun compte n'est requis. Seules les coordonnées (arrondies à 4 décimales) sont envoyées.

## Prérequis

- Android 8.0 (API 26) ou plus récent.
- Pour compiler : JDK 17 ou plus et le SDK Android (plateforme 35).

## Compilation

```bash
./gradlew assembleDebug
```

L'APK est créé dans `app/build/outputs/apk/debug/app-debug.apk`.

Lancer les tests unitaires :

```bash
./gradlew testDebugUnitTest
```

Installer sur un téléphone branché en USB (débogage USB activé) :

```bash
./gradlew installDebug
```

## Permissions

| Permission | Pourquoi |
| --- | --- |
| Localisation (approximative ou précise) | Trouver la météo à l'endroit où vous êtes |
| Notifications | Conseil du matin et alertes pluie |
| Alarmes exactes | Déclencher la notification du matin à l'heure exacte |
| Démarrage du téléphone | Reprogrammer l'alarme après un redémarrage |
| Internet | Télécharger les prévisions |

En arrière-plan, l'application réutilise la dernière position connue. La position est mise à jour à chaque ouverture de l'application. La permission de localisation en arrière-plan n'est donc pas nécessaire. Pensez à ouvrir l'application de temps en temps si vous changez de ville.

Sur certains téléphones (Xiaomi, Huawei, Samsung…), l'optimisation de la batterie peut bloquer les tâches en arrière-plan. Si les alertes n'arrivent pas, désactivez l'optimisation de la batterie pour l'application.

## Architecture

```
app/src/main/java/io/github/octavebenil/personalweather/
├── domain/        Logique métier en Java pur (testée sans Android)
│                  CommuteAdvisor, RainAlertDetector, Forecast, TimeWindow…
├── data/          API Open-Meteo, parsing JSON, cache, réglages, localisation
├── scheduling/    Alarme du matin, service de sonnerie, rafraîchissement horaire
├── notification/  Canaux et notifications
└── ui/            Écran principal, réglages, liste heure par heure
```

## Licence

Distribué sous licence MIT. Voir [LICENSE](LICENSE).
