App = Ember.Application.create({
	rootElement: '#app'
});

App.Router.map(function() {
  // put your routes here
});

App.IndexRoute = Ember.Route.extend({
  model: function() {
	return [
		{ title: 'An analogue of the Szemeredi Regularity Lemma for bounded degree graphs', 
		  summary: 'We show that a sufficiently large graph of bounded degree can be decomposed into quasi-homogeneous pieces. The result can be viewed as a "finitarization" of the classical Farrell-Varadarajan Ergodic Decomposition Theorem.' },
		{ title: 'Drift-diffusion model for spin-polarized transport in a non-degenerate 2DEG controlled by a spin-orbit interaction',
		  summary: 'We apply the Wigner function formalism to derive drift-diffusion transport equations for spin-polarized electrons in a III-V semiconductor single quantum well. Electron spin dynamics is controlled by the linear in momentum spin-orbit interaction. In a studied transport regime an electron momentum scattering rate is appreciably faster than spin dynamics. A set of transport equations is defined in terms of a particle density, spin density, and respective fluxes. The developed model allows studying of coherent dynamics of a non-equilibrium spin polarization. As an example, we consider a stationary transport regime for a heterostructure grown along the (0, 0, 1) crystallographic direction. Due to the interplay of the Rashba and Dresselhaus spin-orbit terms spin dynamics strongly depends on a transport direction. The model is consistent with results of pulse-probe measurement of spin coherence in strained semiconductor layers. It can be useful for studying properties of spin-polarized transport and modeling of spintronic devices operating in the diffusive transport regime.' },
		{ title: 'Optical conductivity of a quasi-one-dimensional system with fluctuating order',
		  summary: 'We describe a formally exact method to calculate the optical conductivity of a one-dimensional system with fluctuating order. For classical phase fluctuations we explicitly determine the optical conductivity by solving two coupled Fokker-Planck equations numerically. Our results differ considerably from perturbation theory and in contrast to Gaussian order parameter fluctuations show a strong dependence on the correlation length.' },
	];
  }
});
