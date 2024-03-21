

  // Two base layers
  var carto = new ol.layer.Geoportail({ layer: 'GEOGRAPHICALGRIDSYSTEMS.PLANIGNV2' })

  // Style function
  var cache = {};
  function style(select){
    return function(f) {
      var style = cache[f.get('img')+'-'+select];
      if (!style) {
        var img = new ol.style.Photo({
          src: '//wsrv.nl/?url=' + f.get('img'),
          radius: select ? 20:15,
          shadow: true,
          stroke: new ol.style.Stroke({
            width: 4,
            color: select ? '#fff':'#fafafa'
          }),
          onload: function() { f.changed(); }
        })
        style = cache[f.get('img')+'-'+select] = new ol.style.Style({
          image: img
        })
      }
      return style;
    }
  };
  
  // GeoJSON layer with a preview attribute
  var vectorSource = new ol.source.Vector({
    url: '../data/fond_guerre.geojson',
    projection: 'EPSG:3857',
    format: new ol.format.GeoJSON(),
		attributions: [ "&copy; <a href='https://data.culture.gouv.fr/explore/dataset/fonds-de-la-guerre-14-18-extrait-de-la-base-memoire'>data.culture.gouv.fr</a>" ],
    logo:"https://www.data.gouv.fr/s/avatars/37/e56718abd4465985ddde68b33be1ef.jpg" 
  });
  var listenerKey = vectorSource.on('change', function(e) {
    if (vectorSource.getState() == 'ready') {
      ol.Observable.unByKey(listenerKey);
      tline.refresh();
    }
  });
  var vector = new ol.layer.Vector({
    name: '1914-18',
    preview: "http://www.culture.gouv.fr/Wave/image/memoire/2445/sap40_z0004141_v.jpg",
    source: vectorSource,
    style: style()
  });


  var map = new ol.Map({
    target: 'map',
    view: new ol.View({
       zoom: 6,
       center: [173664, 6166327]
    }),
    layers: [carto, vector]
   });
   
   // Create Timeline control with interval selection
   var tline = new ol.control.Timeline({
    className: 'ol-zoomhover',
    source: vectorSource,
    graduation: 'day', // 'month'
    zoomButton: true,
    minDate: new Date('2020/5/1'), // Example start date
    maxDate: new Date('2020/5/10'), // Example end date
    interval: '15mn', // Interval between dates
    getHTML: function(f) {
       return '<img src="//wsrv.nl/?url=' + f.get('img') + '"/> ' + (f.get('text') || '');
    },
    getFeatureDate: function(f) {
       return f.get('date');
    },
    endFeatureDate: function(f) {
       var d = f.get('endDate');
       if (!d) {
         d = new Date(f.get('date'));
         d = new Date(d.getTime() + (5 + 10 * Math.random()) * 10 * 24 * 60 * 60 * 1000);
         f.set('endDate', d);
       }
       return d;
    }
   });
   map.addControl(tline);
   
   // Event listener for interval selection
   tline.on('scroll', function(e) {
    var startDate = e.startDate;
    var endDate = e.endDate;
    // Update the interval based on the selected start and end dates
    tline.setInterval(startDate, endDate);
    // Update the displayed dates in the UI
    $('.options .start-date').text(startDate.toLocaleDateString());
    $('.options .end-date').text(endDate.toLocaleDateString());
   });

  var select = new ol.interaction.Select({ hitTolerance: 5, style: style(true) });
  map.addInteraction(select);
  select.on('select', function(e){
    var f = e.selected[0];
    if (f) {
      tline.setDate(f);
    }
  });

  