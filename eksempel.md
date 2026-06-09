# Eksempel på respons fra API-et

Scenario: bruker søker AAP 1/8-25. Det innvilges sykepengeerstatning fram til
nyttår og deretter ordinær AAP.

Responsen fra maksimum-endepunktet blir da (om man spør API-et etter 1/1-2026):

```json
{
  "vedtak": [
    {
      "dagsats": 1022,
      "dagsatsEtterUføreReduksjon": 1022,
      "vedtakId": "59866",
      "status": "LØPENDE",
      "saksnummer": "LoCAL_4LHHPQ0",
      "vedtaksdato": "2026-06-09",
      "periode": {
        "fraOgMedDato": "2025-08-01",
        "tilOgMedDato": "2025-12-31"
      },
      "rettighetsType": "SYKEPENGEERSTATNING",
      "beregningsgrunnlag": 213375,
      "barnMedStonad": 2,
      "barnetillegg": 74,
      "barnetilleggSats": 37,
      "kildesystem": "KELVIN",
      "samordningsId": null,
      "opphorsAarsak": null,
      "vedtaksTypeKode": "O",
      "vedtaksTypeNavn": null,
      "utbetaling": [
        {
          "reduksjon": null,
          "utbetalingsgrad": 100,
          "periode": {
            "fraOgMedDato": "2025-08-01",
            "tilOgMedDato": "2025-12-31"
          },
          "belop": 119464,
          "dagsats": 1022,
          "barnetillegg": 74,
          "barnetilegg": 74
        }
      ]
    },
    {
      "dagsats": 1022,
      "dagsatsEtterUføreReduksjon": 1022,
      "vedtakId": "59866",
      "status": "LØPENDE",
      "saksnummer": "LoCAL_4LHHPQ0",
      "vedtaksdato": "2026-06-09",
      "periode": {
        "fraOgMedDato": "2026-01-01",
        "tilOgMedDato": "2026-04-30"
      },
      "rettighetsType": "BISTANDSBEHOV",
      "beregningsgrunnlag": 213375,
      "barnMedStonad": 2,
      "barnetillegg": 76,
      "barnetilleggSats": 38,
      "kildesystem": "KELVIN",
      "samordningsId": null,
      "opphorsAarsak": null,
      "vedtaksTypeKode": "O",
      "vedtaksTypeNavn": null,
      "utbetaling": [
        {
          "reduksjon": null,
          "utbetalingsgrad": 100,
          "periode": {
            "fraOgMedDato": "2026-01-01",
            "tilOgMedDato": "2026-04-30"
          },
          "belop": 94428,
          "dagsats": 1022,
          "barnetillegg": 76,
          "barnetilegg": 76
        }
      ]
    },
    {
      "dagsats": 1072,
      "dagsatsEtterUføreReduksjon": 1072,
      "vedtakId": "59866",
      "status": "LØPENDE",
      "saksnummer": "LoCAL_4LHHPQ0",
      "vedtaksdato": "2026-06-09",
      "periode": {
        "fraOgMedDato": "2026-05-01",
        "tilOgMedDato": "2026-07-31"
      },
      "rettighetsType": "BISTANDSBEHOV",
      "beregningsgrunnlag": 213375,
      "barnMedStonad": 2,
      "barnetillegg": 76,
      "barnetilleggSats": 38,
      "kildesystem": "KELVIN",
      "samordningsId": null,
      "opphorsAarsak": null,
      "vedtaksTypeKode": "O",
      "vedtaksTypeNavn": null,
      "utbetaling": [
        {
          "reduksjon": null,
          "utbetalingsgrad": 100,
          "periode": {
            "fraOgMedDato": "2026-05-01",
            "tilOgMedDato": "2026-05-31"
          },
          "belop": 24108,
          "dagsats": 1072,
          "barnetillegg": 76,
          "barnetilegg": 76
        },
        {
          "reduksjon": null,
          "utbetalingsgrad": 85,
          "periode": {
            "fraOgMedDato": "2026-06-01",
            "tilOgMedDato": "2026-06-09"
          },
          "belop": 6825,
          "dagsats": 911,
          "barnetillegg": 65,
          "barnetilegg": 65
        }
      ]
    }
  ]
}
```

Merk at samme vedtakId brukes på begge elementene i listen. Dette er fordi vi
splitter responsen etter ny type rettighet.

Merk også at siste "vedtak" har to elementer i utbetaling-listen. Dette er fordi dagsatsen endrer seg fra 1 mai på grunn av G-regulering.

Om man spør API-et før 2026 (her 2025-12-31), får man denne responsen:

```json
{
  "vedtak": [
    {
      "dagsats": 1022,
      "dagsatsEtterUføreReduksjon": 1022,
      "vedtakId": "59866",
      "status": "LØPENDE",
      "saksnummer": "LoCAL_4LHHPQ0",
      "vedtaksdato": "2026-06-09",
      "periode": {
        "fraOgMedDato": "2025-08-01",
        "tilOgMedDato": "2025-12-31"
      },
      "rettighetsType": "SYKEPENGEERSTATNING",
      "beregningsgrunnlag": 213375,
      "barnMedStonad": 2,
      "barnetillegg": 74,
      "barnetilleggSats": 37,
      "kildesystem": "KELVIN",
      "samordningsId": null,
      "opphorsAarsak": null,
      "vedtaksTypeKode": "O",
      "vedtaksTypeNavn": null,
      "utbetaling": [
        {
          "reduksjon": null,
          "utbetalingsgrad": 100,
          "periode": {
            "fraOgMedDato": "2025-08-01",
            "tilOgMedDato": "2025-12-31"
          },
          "belop": 119464,
          "dagsats": 1022,
          "barnetillegg": 74,
          "barnetilegg": 74
        }
      ]
    },
    {
      "dagsats": 1022,
      "dagsatsEtterUføreReduksjon": 1022,
      "vedtakId": "59866",
      "status": "LØPENDE",
      "saksnummer": "LoCAL_4LHHPQ0",
      "vedtaksdato": "2026-06-09",
      "periode": {
        "fraOgMedDato": "2026-01-01",
        "tilOgMedDato": "2026-04-30"
      },
      "rettighetsType": "BISTANDSBEHOV",
      "beregningsgrunnlag": 213375,
      "barnMedStonad": 2,
      "barnetillegg": 76,
      "barnetilleggSats": 38,
      "kildesystem": "KELVIN",
      "samordningsId": null,
      "opphorsAarsak": null,
      "vedtaksTypeKode": "O",
      "vedtaksTypeNavn": null,
      "utbetaling": []
    },
    {
      "dagsats": 1072,
      "dagsatsEtterUføreReduksjon": 1072,
      "vedtakId": "59866",
      "status": "LØPENDE",
      "saksnummer": "LoCAL_4LHHPQ0",
      "vedtaksdato": "2026-06-09",
      "periode": {
        "fraOgMedDato": "2026-05-01",
        "tilOgMedDato": "2026-07-31"
      },
      "rettighetsType": "BISTANDSBEHOV",
      "beregningsgrunnlag": 213375,
      "barnMedStonad": 2,
      "barnetillegg": 76,
      "barnetilleggSats": 38,
      "kildesystem": "KELVIN",
      "samordningsId": null,
      "opphorsAarsak": null,
      "vedtaksTypeKode": "O",
      "vedtaksTypeNavn": null,
      "utbetaling": []
    }
  ]
}
```

Merk at da får man ikke noen informasjon om framtidige utbetalinger.