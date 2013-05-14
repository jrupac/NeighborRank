App = Ember.Application.create({
	LOG_TRANSITIONS: true,
	relevant: new Em.Set(),
	itemMap: new Em.Map(),
});

updateStats = function() {
    $('#vm-rec').text(' ' + App.FullLuceneResultsController.recall());
    $('#vm-prec').text(App.FullLuceneResultsController.precision());
    $('#vm-dcg').text(App.FullLuceneResultsController.dcg());
    $('#vm-fscore').text(App.FullLuceneResultsController.fscore());
    $('#vm-reciprank').text(App.FullLuceneResultsController.reciprank());

    $('#hm-rec').text(App.MixedResultsController.recall());
    $('#hm-prec').text(App.MixedResultsController.precision());
    $('#hm-dcg').text(App.MixedResultsController.dcg());
    $('#hm-fscore').text(App.MixedResultsController.fscore());
    $('#hm-reciprank').text(App.MixedResultsController.reciprank());
}

Ember.TextSupport.reopen({
  attributeBindings: ["autofocus", "class", "id"]
});

App.Searchbox = Ember.TextField.extend({
	placeholder: "Type your query (quantum, algorithms, ...)",
	id: "appendedInputButton",
	type: "text",
	autofocus: "autofocus",
	insertNewline: function() {
	    console.log("newline!");
        this.get('controller').doSearch()
	}
});

// Result model
App.Result = Ember.Object.extend({
    docid: null,
    title: null,
    summary: null,
    tableName: null,
    slug: function() {
        return this.get('tableName') + '-' + this.get('docid')
    }.property('docid', 'tableName').cacheable(),
    href: function() {
        return '#' + this.get('slug')
    }.property('slug').cacheable(),
    relevanceClass: null
});

App.ResultsController = Ember.ArrayController.extend({
    addRelevance: function(id) {
        if (this.contentMap.has(id)) {
            this.contentMap.get(id).set('relevanceClass', 'relevant');
        }
    },
    removeRelevance: function(id) {
       if (this.contentMap.has(id)) {
            this.contentMap.get(id).set('relevanceClass', null);
        }
    },
    query: function(query, K) {
        if (query === undefined) { return; }
        console.log("fetching...");
        _this = this;
	    $.post(
	        "http://localhost:8080/results/",
	        JSON.stringify({'query': query, 'K': this.K || K, 'M': this.M}),
	        this.process,
	        'json')
	     .fail(this.fail);
    },
    fail: function(x, y) {
        console.log('here');
    },
    precision: function() {
        inter = 0.0 + this.get('content').filter(function(item, index, e) {
            return App.relevant.contains(item.docid);
        }).get('length');

        return inter / this.get('content').get('length');
    },
    recall: function() {
        inter = 0.0 + this.get('content').filter(function(item, index, e) {
            return App.relevant.contains(item.docid);
        }).get('length');

        return inter / App.relevant.length;
    },
    dcg: function() {
        val = 0.0;
        this.get('content').forEach(function(item, index, e) {
            if (App.relevant.contains(item.docid)) {
                val += 1.0 / Math.log(index + 2);
            }
        });
        return val;
    },
    fscore: function() {
        r = this.recall();
        p = this.precision();
        return (2.0 * r * p) / (r + p);
    },
    reciprank: function() {
        idx = 0;
        this.get('content').find(function(item, index, e) {
            if (App.relevant.contains(item.docid)) {
                idx = index + 1;
                return true;
            } else {
                return false;
            }
        });
        return 1.0 / idx;
    },
});

App.FullLuceneResultsController = App.ResultsController.create({
    content: [],
    contentMap: Em.Map.create(),
    K: 0,
    M: 30,
    process: function(response) {
        App.FullLuceneResultsController.clear();
        i = 0;
        response.results.forEach(function(child) {
            newResult = App.Result.create(child);
            newResult.set('tableName', 'lucene' + i);
            i++;
            App.FullLuceneResultsController.pushObject(newResult);
            App.FullLuceneResultsController.contentMap.set(child.docid, newResult);
        })
    }
});

App.MixedResultsController = App.ResultsController.create({
    content: [],
    contentMap: Em.Map.create(),
    K: null,
    M: 30,
    process: function(response) {
        App.MixedResultsController.clear();
        i = 0;
        response.results.forEach(function(child) {
            newResult = App.Result.create(child);
            newResult.set('tableName', 'mix' + i);
            i++;
            App.MixedResultsController.pushObject(newResult);
            App.MixedResultsController.contentMap.set(child.docid, newResult);
        })
    }
});

App.Router.map(function() {
    this.route('index', { path: '/'});
});

App.IndexView = Ember.View.extend({
	dcg: function() {
	    console.log('i fired too' + this.get('controller.dcg'));
	    return this.get('controller.dcg')
	}.observes('controller.dcg').property()
});

App.IndexController = Ember.Controller.extend({
	query: '',
	toggleRelevance: function(item) {
        rels = App.relevant;
        id = item.get('docid');

        if (!rels.contains(id)) {
            rels.add(id);
            App.FullLuceneResultsController.addRelevance(id);
            App.MixedResultsController.addRelevance(id);
        } else {
            rels.remove(id);
            App.FullLuceneResultsController.removeRelevance(id);
            App.MixedResultsController.removeRelevance(id);
        }
	},
	doSearch: function() {
	    App.FullLuceneResultsController.query(this.query);
	    App.MixedResultsController.query(this.query, $('#k-value').val());
	    updateStats();
	    $('.data-table').css('display', 'none');
	},
	computeStats: function() {
	    updateStats();
	    $('.data-table').css('display', 'inline');
	}
});