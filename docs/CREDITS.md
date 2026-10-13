# Credits & Licenses

All animal photographs in this app are sourced from **Wikimedia Commons** under a
Creative Commons license (or public domain). They are used here solely as
illustrative photos of each species; the scientific model of each animal's
vision lives in `core/`.

| Animal id | Photo (Wikimedia) | License | Author |
|---|---|---|---|
| `human` | Iris of human male | CC BY-SA 4.0 | Jürgen Haase / Birgit Moss (Wikimedia Commons) |
| `dog` | Resting dog face | CC0 1.0 | Caleb Rooney (AFP, Wikimedia Commons) |
| `cat` | Cat Face 02 | CC BY-SA 4.0 | Mikko Savela (Flickr) |
| `horse` | Horse portrait, Ukraine | CC BY-SA 4.0 | André Karwath (Wikimedia Commons) |
| `eagle` | Bald Eagle Portrait | CC BY 3.0 | U.S. Fish and Wildlife Service (Flickr) |
| `bee` | Honey Bee Flower | CC BY-SA 4.0 | Andy Anderson (Flickr) |
| `snake` | Apicta mature | CC BY-SA 4.0 | Dirk Ercken (Flickr) |
| `pigeon` | Columba livia | CC BY-SA 4.0 | Sanja Asakov (Flickr) |
| `chicken` | Portrait of rooster | CC BY-SA 4.0 | David R / Jules Vernex (Wikimedia Commons) |
| `butterfly` | Papilio xuthus (in flight) | CC BY-SA 4.0 | Jan G. (Flickr) |
| `fly` | Drosophila melanogaster under a microscope | CC BY-SA 4.0 | The Public Domain Review (Flickr) |
| `octopus` | Octopus vulgaris | Public domain (pre-1923 US publication) | Public domain (Encyclopædia Britannica, 1911) |
| `mantisshrimp` | Mantis shrimp (Odontodactylus scyllarus) | CC BY-SA 4.0 | Bernard Dupont (Wikimedia Commons) |
| `dolphin` | Tursiops truncatus | Public domain | United States Government (NOAA) |

## Usage notes
- **CC BY-SA 4.0 / CC BY 3.0** images must be attributed with the author and
  license notice. This file serves as the attribution source for the app and
  for anyone reusing the photos.
- **CC0 1.0** images are dedicated to the public domain.
- **Public domain** images have no copyright restrictions.
- Photos are stored in `app/src/main/assets/animals/<id>.jpg` and rendered in
  the selector as circular 56dp crops with the species glyph as a fallback while
  the photo loads or on load failure.

## Source mapping (id → Wikimedia file)
- Human: https://commons.wikimedia.org/wiki/File:Iris_of_human_male.jpg
- Dog: https://commons.wikimedia.org/wiki/File:Resting_dog_face.jpg
- Cat: https://commons.wikimedia.org/wiki/File:Cat_Face_02.JPG
- Horse: https://commons.wikimedia.org/wiki/File:Horse_portrait,_Ukraine.jpg
- Eagle: https://commons.wikimedia.org/wiki/File:Bald_Eagle_Portrait_(120773677).jpeg
- Bee: https://commons.wikimedia.org/wiki/File:Honey_Bee_Flower.jpg
- Snake: https://commons.wikimedia.org/wiki/File:Apicta_mature.jpg
- Pigeon: https://commons.wikimedia.org/wiki/File:Columba_livia_-_01.jpg
- Chicken: https://commons.wikimedia.org/wiki/File:Portrait_of_rooster_(Gallus_domesticus).jpg
- Butterfly: https://commons.wikimedia.org/wiki/File:Papilio_xuthus_(in_flight).JPG
- Fly: https://commons.wikimedia.org/wiki/File:Drosophila_melanogaster_under_a_microscope_01.jpg
- Octopus: https://commons.wikimedia.org/wiki/File:Octopus_vulgaris_Merculiano.jpg
- Mantis shrimp: https://commons.wikimedia.org/wiki/File:Camarón_mantis_(Odontodactylus_scyllarus),_Anilao,_Filipinas,_2023-08-24,_DD_164.jpg
- Dolphin: https://commons.wikimedia.org/wiki/File:Tursiops_truncatus_01.jpg
