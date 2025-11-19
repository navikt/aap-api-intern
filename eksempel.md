# Eksempel på respons fra API-et

Scenario: bruker søker AAP 19/11-25. Det innvilges sykepengeerstatning fram til
nyttår og deretter ordinær AAP.

Responsen blir da:

(om man spør API-et etter 1/1-2026)
```json
{
  "vedtak" : [ {
    "dagsats" : 1022,
    "dagsatsEtterUføreReduksjon" : 1022,
    "vedtakId" : "2",
    "status" : "LØPENDE",
    "saksnummer" : "4LDQPDS",
    "vedtaksdato" : "2025-11-19",
    "periode" : {
      "fraOgMedDato" : "2025-11-19",
      "tilOgMedDato" : "2025-12-31"
    },
    "rettighetsType" : "SYKEPENGEERSTATNING",
    "beregningsgrunnlag" : 212984,
    "barnMedStonad" : 0,
    "barnetillegg" : 0,
    "kildesystem" : "KELVIN",
    "samordningsId" : null,
    "opphorsAarsak" : null,
    "vedtaksTypeKode" : null,
    "vedtaksTypeNavn" : null,
    "utbetaling" : [ {
      "reduksjon" : null,
      "utbetalingsgrad" : 100,
      "periode" : {
        "fraOgMedDato" : "2025-11-19",
        "tilOgMedDato" : "2025-11-30"
      },
      "belop" : 8176,
      "dagsats" : 1022,
      "barnetilegg" : 0,
      "barnetillegg" : 0
    }, {
      "reduksjon" : null,
      "utbetalingsgrad" : 100,
      "periode" : {
        "fraOgMedDato" : "2025-12-01",
        "tilOgMedDato" : "2025-12-14"
      },
      "belop" : 10220,
      "dagsats" : 1022,
      "barnetilegg" : 0,
      "barnetillegg" : 0
    }, {
      "reduksjon" : null,
      "utbetalingsgrad" : 100,
      "periode" : {
        "fraOgMedDato" : "2025-12-15",
        "tilOgMedDato" : "2025-12-28"
      },
      "belop" : 10220,
      "dagsats" : 1022,
      "barnetilegg" : 0,
      "barnetillegg" : 0
    }, {
      "reduksjon" : null,
      "utbetalingsgrad" : 100,
      "periode" : {
        "fraOgMedDato" : "2025-12-29",
        "tilOgMedDato" : "2025-12-31"
      },
      "belop" : 3066,
      "dagsats" : 1022,
      "barnetilegg" : 0,
      "barnetillegg" : 0
    } ]
  }, {
    "dagsats" : 1022,
    "dagsatsEtterUføreReduksjon" : 1022,
    "vedtakId" : "2",
    "status" : "LØPENDE",
    "saksnummer" : "4LDQPDS",
    "vedtaksdato" : "2025-11-19",
    "periode" : {
      "fraOgMedDato" : "2026-01-01",
      "tilOgMedDato" : "2026-11-18"
    },
    "rettighetsType" : "BISTANDSBEHOV",
    "beregningsgrunnlag" : 212984,
    "barnMedStonad" : 0,
    "barnetillegg" : 0,
    "kildesystem" : "KELVIN",
    "samordningsId" : null,
    "opphorsAarsak" : null,
    "vedtaksTypeKode" : null,
    "vedtaksTypeNavn" : null,
    "utbetaling" : [ {
      "reduksjon" : null,
      "utbetalingsgrad" : 100,
      "periode" : {
        "fraOgMedDato" : "2026-01-01",
        "tilOgMedDato" : "2026-01-11"
      },
      "belop" : 7154,
      "dagsats" : 1022,
      "barnetilegg" : 0,
      "barnetillegg" : 0
    }, {
      "reduksjon" : null,
      "utbetalingsgrad" : 100,
      "periode" : {
        "fraOgMedDato" : "2026-01-12",
        "tilOgMedDato" : "2026-01-25"
      },
      "belop" : 10220,
      "dagsats" : 1022,
      "barnetilegg" : 0,
      "barnetillegg" : 0
    }, {
      "reduksjon" : null,
      "utbetalingsgrad" : 100,
      "periode" : {
        "fraOgMedDato" : "2026-01-26",
        "tilOgMedDato" : "2026-02-08"
      },
      "belop" : 10220,
      "dagsats" : 1022,
      "barnetilegg" : 0,
      "barnetillegg" : 0
    }, {
      "reduksjon" : null,
      "utbetalingsgrad" : 100,
      "periode" : {
        "fraOgMedDato" : "2026-02-09",
        "tilOgMedDato" : "2026-02-22"
      },
      "belop" : 10220,
      "dagsats" : 1022,
      "barnetilegg" : 0,
      "barnetillegg" : 0
    }, {
      "reduksjon" : null,
      "utbetalingsgrad" : 100,
      "periode" : {
        "fraOgMedDato" : "2026-02-23",
        "tilOgMedDato" : "2026-03-08"
      },
      "belop" : 10220,
      "dagsats" : 1022,
      "barnetilegg" : 0,
      "barnetillegg" : 0
    }, {
      "reduksjon" : null,
      "utbetalingsgrad" : 100,
      "periode" : {
        "fraOgMedDato" : "2026-03-09",
        "tilOgMedDato" : "2026-03-22"
      },
      "belop" : 10220,
      "dagsats" : 1022,
      "barnetilegg" : 0,
      "barnetillegg" : 0
    }, {
      "reduksjon" : null,
      "utbetalingsgrad" : 100,
      "periode" : {
        "fraOgMedDato" : "2026-03-23",
        "tilOgMedDato" : "2026-04-05"
      },
      "belop" : 10220,
      "dagsats" : 1022,
      "barnetilegg" : 0,
      "barnetillegg" : 0
    }, {
      "reduksjon" : null,
      "utbetalingsgrad" : 100,
      "periode" : {
        "fraOgMedDato" : "2026-04-06",
        "tilOgMedDato" : "2026-04-19"
      },
      "belop" : 10220,
      "dagsats" : 1022,
      "barnetilegg" : 0,
      "barnetillegg" : 0
    }, {
      "reduksjon" : null,
      "utbetalingsgrad" : 100,
      "periode" : {
        "fraOgMedDato" : "2026-04-20",
        "tilOgMedDato" : "2026-05-03"
      },
      "belop" : 10220,
      "dagsats" : 1022,
      "barnetilegg" : 0,
      "barnetillegg" : 0
    }, {
      "reduksjon" : null,
      "utbetalingsgrad" : 100,
      "periode" : {
        "fraOgMedDato" : "2026-05-04",
        "tilOgMedDato" : "2026-05-17"
      },
      "belop" : 10220,
      "dagsats" : 1022,
      "barnetilegg" : 0,
      "barnetillegg" : 0
    }, {
      "reduksjon" : null,
      "utbetalingsgrad" : 100,
      "periode" : {
        "fraOgMedDato" : "2026-05-18",
        "tilOgMedDato" : "2026-05-31"
      },
      "belop" : 10220,
      "dagsats" : 1022,
      "barnetilegg" : 0,
      "barnetillegg" : 0
    }, {
      "reduksjon" : null,
      "utbetalingsgrad" : 100,
      "periode" : {
        "fraOgMedDato" : "2026-06-01",
        "tilOgMedDato" : "2026-06-14"
      },
      "belop" : 10220,
      "dagsats" : 1022,
      "barnetilegg" : 0,
      "barnetillegg" : 0
    }, {
      "reduksjon" : null,
      "utbetalingsgrad" : 100,
      "periode" : {
        "fraOgMedDato" : "2026-06-15",
        "tilOgMedDato" : "2026-06-28"
      },
      "belop" : 10220,
      "dagsats" : 1022,
      "barnetilegg" : 0,
      "barnetillegg" : 0
    }, {
      "reduksjon" : null,
      "utbetalingsgrad" : 100,
      "periode" : {
        "fraOgMedDato" : "2026-06-29",
        "tilOgMedDato" : "2026-07-12"
      },
      "belop" : 10220,
      "dagsats" : 1022,
      "barnetilegg" : 0,
      "barnetillegg" : 0
    }, {
      "reduksjon" : null,
      "utbetalingsgrad" : 100,
      "periode" : {
        "fraOgMedDato" : "2026-07-13",
        "tilOgMedDato" : "2026-07-26"
      },
      "belop" : 10220,
      "dagsats" : 1022,
      "barnetilegg" : 0,
      "barnetillegg" : 0
    }, {
      "reduksjon" : null,
      "utbetalingsgrad" : 100,
      "periode" : {
        "fraOgMedDato" : "2026-07-27",
        "tilOgMedDato" : "2026-08-09"
      },
      "belop" : 10220,
      "dagsats" : 1022,
      "barnetilegg" : 0,
      "barnetillegg" : 0
    }, {
      "reduksjon" : null,
      "utbetalingsgrad" : 100,
      "periode" : {
        "fraOgMedDato" : "2026-08-10",
        "tilOgMedDato" : "2026-08-23"
      },
      "belop" : 10220,
      "dagsats" : 1022,
      "barnetilegg" : 0,
      "barnetillegg" : 0
    }, {
      "reduksjon" : null,
      "utbetalingsgrad" : 100,
      "periode" : {
        "fraOgMedDato" : "2026-08-24",
        "tilOgMedDato" : "2026-09-06"
      },
      "belop" : 10220,
      "dagsats" : 1022,
      "barnetilegg" : 0,
      "barnetillegg" : 0
    }, {
      "reduksjon" : null,
      "utbetalingsgrad" : 100,
      "periode" : {
        "fraOgMedDato" : "2026-09-07",
        "tilOgMedDato" : "2026-09-20"
      },
      "belop" : 10220,
      "dagsats" : 1022,
      "barnetilegg" : 0,
      "barnetillegg" : 0
    }, {
      "reduksjon" : null,
      "utbetalingsgrad" : 100,
      "periode" : {
        "fraOgMedDato" : "2026-09-21",
        "tilOgMedDato" : "2026-10-04"
      },
      "belop" : 10220,
      "dagsats" : 1022,
      "barnetilegg" : 0,
      "barnetillegg" : 0
    }, {
      "reduksjon" : null,
      "utbetalingsgrad" : 100,
      "periode" : {
        "fraOgMedDato" : "2026-10-05",
        "tilOgMedDato" : "2026-10-18"
      },
      "belop" : 10220,
      "dagsats" : 1022,
      "barnetilegg" : 0,
      "barnetillegg" : 0
    }, {
      "reduksjon" : null,
      "utbetalingsgrad" : 100,
      "periode" : {
        "fraOgMedDato" : "2026-10-19",
        "tilOgMedDato" : "2026-11-01"
      },
      "belop" : 10220,
      "dagsats" : 1022,
      "barnetilegg" : 0,
      "barnetillegg" : 0
    }, {
      "reduksjon" : null,
      "utbetalingsgrad" : 100,
      "periode" : {
        "fraOgMedDato" : "2026-11-02",
        "tilOgMedDato" : "2026-11-15"
      },
      "belop" : 10220,
      "dagsats" : 1022,
      "barnetilegg" : 0,
      "barnetillegg" : 0
    }, {
      "reduksjon" : null,
      "utbetalingsgrad" : 100,
      "periode" : {
        "fraOgMedDato" : "2026-11-16",
        "tilOgMedDato" : "2026-11-18"
      },
      "belop" : 3066,
      "dagsats" : 1022,
      "barnetilegg" : 0,
      "barnetillegg" : 0
    } ]
  } ]
}
```

Merk at samme vedtakId brukes på begge elementene i listen. Dette er fordi vi
splitter responsen etter ny type rettighet.

Om man spør API-et før 2026 (her 2025-12-13), får man denne responsen:

```json
{
  "vedtak": [
    {
      "dagsats": 1022,
      "dagsatsEtterUføreReduksjon": 1022,
      "vedtakId": "2",
      "status": "LØPENDE",
      "saksnummer": "4LDQPDS",
      "vedtaksdato": "2025-11-19",
      "periode": {
        "fraOgMedDato": "2025-11-19",
        "tilOgMedDato": "2025-12-31"
      },
      "rettighetsType": "SYKEPENGEERSTATNING",
      "beregningsgrunnlag": 212984,
      "barnMedStonad": 0,
      "barnetillegg": 0,
      "kildesystem": "KELVIN",
      "samordningsId": null,
      "opphorsAarsak": null,
      "vedtaksTypeKode": null,
      "vedtaksTypeNavn": null,
      "utbetaling": [
        {
          "reduksjon": null,
          "utbetalingsgrad": 100,
          "periode": {
            "fraOgMedDato": "2025-11-19",
            "tilOgMedDato": "2025-11-30"
          },
          "belop": 8176,
          "dagsats": 1022,
          "barnetilegg": 0,
          "barnetillegg": 0
        }
      ]
    },
    {
      "dagsats": 1022,
      "dagsatsEtterUføreReduksjon": 1022,
      "vedtakId": "2",
      "status": "LØPENDE",
      "saksnummer": "4LDQPDS",
      "vedtaksdato": "2025-11-19",
      "periode": {
        "fraOgMedDato": "2026-01-01",
        "tilOgMedDato": "2026-11-18"
      },
      "rettighetsType": "BISTANDSBEHOV",
      "beregningsgrunnlag": 212984,
      "barnMedStonad": 0,
      "barnetillegg": 0,
      "kildesystem": "KELVIN",
      "samordningsId": null,
      "opphorsAarsak": null,
      "vedtaksTypeKode": null,
      "vedtaksTypeNavn": null,
      "utbetaling": []
    }
  ]
}
```

