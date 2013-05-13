App = Ember.Application.create({
	LOG_TRANSITIONS: true,
	comparing: false,
	relevant: new Ember.Set(),
});

Ember.TextSupport.reopen({
  attributeBindings: ["autofocus", "class", "id"]
});

App.Searchbox = Ember.TextField.extend({
	placeholder: "Type your query (quantum, algorithms, ...)",
	id: "appendedInputButton",
	type: "text",
	autofocus: "autofocus"
});

// Result model
App.Result = Ember.Object.extend({
    docid: null,
    title: null,
    summary: null,
    relevanceClass: function() {
        if (App.relevant.contains(this.docid)) {
            return 'relevant';
        } else {
            return null;
        }
    }.property(this.docid)
});

App.ResultsController = Ember.ArrayController.extend({
    query: function(query, K, M) {
        if (query === undefined) { return; }
        _this = this;
	    $.post(
	        "http://localhost:8080/results/",
	        JSON.stringify({'query': query, 'K': K, 'M': M}))
	     .done(this.process)
	     .fail(this.fail);
    },
    fail: function(x, y) {
        console.log('here');
    }
});

App.FullLuceneResultsController = App.ResultsController.create({
    content: [],
    K: 0,
    M: 30,
    process: function(response) {
        App.FullLuceneResultsController.clear();
        console.log('here1');
        response.results.forEach(function(child) {
            App.FullLuceneResultsController.pushObject(App.Result.create(child));
        })
    }
});

App.MixedResultsController = App.ResultsController.create({
    content: [],
    K: 5,
    M: 6,
    process: function(response) {
        console.log(this);
        App.MixedResultsController.clear();
        console.log('here1');
        response.results.forEach(function(child) {
            App.MixedResultsController.pushObject(App.Result.create(child));
        })
        console.log(JSON.stringify(_this.content));
    }
});

App.FullNNResultsController = App.ResultsController.create({
    content: [],
    K: 29,
    M: 1,
    process: function(response) {
        App.FullNNResultsController.clear();
        console.log('here1');
        response.results.forEach(function(child) {
            App.FullNNResultsController.pushObject(App.Result.create(child));
        })
        console.log(JSON.stringify(_this.content));
    }
});

//
//App.Result.FIXTURES = [
//    {
//    id: 10,
//    docid: 1,
//    title: 'An analogue of the Szemeredi Regularity Lemma for bounded degree graphs',
//    summary: 'We show that a sufficiently large graph of bounded degree can be decomposed into quasi-homogeneous pieces. The result can be viewed as a "finitarization" of the classical Farrell-Varadarajan Ergodic Decomposition Theorem.',
//    },
//    {
//    id: 11,
//    docid: 2,
//    title: 'Drift-diffusion model for spin-polarized transport in a non-degenerate 2DEG controlled by a spin-orbit interaction',
//    summary: 'We apply the Wigner function formalism to derive drift-diffusion transport equations for spin-polarized electrons in a III-V semiconductor single quantum well. Electron spin dynamics is controlled by the linear in momentum spin-orbit interaction. In a studied transport regime an electron momentum scattering rate is appreciably faster than spin dynamics. A set of transport equations is defined in terms of a particle density, spin density, and respective fluxes. The developed model allows studying of coherent dynamics of a non-equilibrium spin polarization. As an example, we consider a stationary transport regime for a heterostructure grown along the (0, 0, 1) crystallographic direction. Due to the interplay of the Rashba and Dresselhaus spin-orbit terms spin dynamics strongly depends on a transport direction. The model is consistent with results of pulse-probe measurement of spin coherence in strained semiconductor layers. It can be useful for studying properties of spin-polarized transport and modeling of spintronic devices operating in the diffusive transport regime.',
//    },
//    {
//    id: 12,
//    docid: 3,
//    title: 'Optical conductivity of a quasi-one-dimensional system with fluctuating order',
//    summary: 'We describe a formally exact method to calculate the optical conductivity of a one-dimensional system with fluctuating order. For classical phase fluctuations we explicitly determine the optical conductivity by solving two coupled Fokker-Planck equations numerically. Our results differ considerably from perturbation theory and in contrast to Gaussian order parameter fluctuations show a strong dependence on the correlation length.',
//    }
//];

App.Router.map(function() {
    this.route('index', { path: '/'});
});


App.ResultsCollectionView = Ember.CollectionView.extend({
    itemViewClass: Ember.View.extend({
        relevanceClass: function() {
            if (App.relevant.contains(this.get('content.docid'))) {
                return 'relevant';
            } else { return null; }
        }.property('content.docid')
    })
});


App.IndexView = Ember.View.extend({
    resultCheckbox: Ember.Checkbox.extend({
        checked: false,
        checkedObserver: function(x, y) {
            item = this.get('content');
            this.get('controller').toggleRelevance(item.get('docid'))
        }.observes('checked')
    })
});

App.IndexController = Ember.Controller.extend({
	query: '',
	toggleRelevance: function(id) {
        rels = App.get('relevant');
        if (rels.contains(id)) {
            rels.remove(id);
        } else {
            rels.add(id);
        }
	},
	doSearch: function() {
	    console.log("getting stuff... for " + this.query);
	    App.FullLuceneResultsController.query(this.query,
	        App.FullLuceneResultsController.K,
	        App.FullLuceneResultsController.M);
	},
	doComparison: function() {
	    console.log("doing comparison...");
	    App.FullNNResultsController.query(this.query,
	        App.FullNNResultsController.K,
	        App.FullNNResultsController.M);
	    App.MixedResultsController.query(this.query,
	        App.MixedResultsController.K,
	        App.MixedResultsController.M);
	    App.set('comparing', true);
	}
});
